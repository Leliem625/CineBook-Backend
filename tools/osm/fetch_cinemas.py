#!/usr/bin/env python3
"""
Lấy danh sách rạp chiếu phim tại Việt Nam từ OpenStreetMap (Overpass API),
chuẩn hóa và ghi snapshot cho CinemaSeeder.

Dữ liệu © OpenStreetMap contributors, giấy phép ODbL - phải ghi nguồn trên website.

Cách dùng (chạy từ thư mục cinebook-backend):
    python3 tools/osm/fetch_cinemas.py                      # gọi Overpass
    python3 tools/osm/fetch_cinemas.py --from-file raw.json # dùng kết quả đã tải
Kết quả: src/main/resources/seed/osm-cinemas-vn.json

Chỉ chạy khi cần cập nhật dữ liệu; không gọi API này mỗi lần khởi động app.
"""
import argparse
import json
import math
import re
import sys
import time
import unicodedata
import urllib.parse
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

HERE = Path(__file__).resolve().parent
QUERY_FILE = HERE / "cinemas_by_province.overpassql"
OUT_FILE = HERE.parent.parent / "src/main/resources/seed/osm-cinemas-vn.json"
OVERPASS_URL = "https://overpass-api.de/api/interpreter"

# Hãng rạp: (từ khóa nhận diện trong brand/operator/name, tên chuẩn)
CHAINS = [
    ("cgv", "CGV Cinemas"),
    ("lotte", "Lotte Cinema"),
    ("galaxy", "Galaxy Cinema"),
    ("bhd", "BHD Star Cineplex"),
    ("beta", "Beta Cinemas"),
    ("cinestar", "Cinestar"),
    ("mega gs", "Mega GS Cinemas"),
    ("starlight", "Starlight"),
    ("platinum", "Platinum Cineplex"),
    ("rio", "Rio Cinemas"),
    ("dcine", "DCINE"),
    ("cinemax", "Cinemax"),
    ("touch cinema", "Touch Cinema"),
]
INDEPENDENT = "Rạp độc lập"

# Địa điểm bị gắn nhầm thẻ amenity=cinema trên OSM
EXCLUDE = re.compile(r"karaoke|tiệm ảnh|ảnh màu|studio ảnh", re.IGNORECASE)


def slugify(text: str) -> str:
    text = text.replace("đ", "d").replace("Đ", "D")
    text = unicodedata.normalize("NFD", text)
    text = "".join(c for c in text if unicodedata.category(c) != "Mn")
    return re.sub(r"[^a-z0-9]+", "-", text.lower()).strip("-")


def province_name(osm_name: str) -> str:
    return re.sub(r"^(Thành phố|Tỉnh)\s+", "", osm_name)


def detect_chain(tags: dict):
    # Ưu tiên tên rạp; thẻ brand/operator trên OSM đôi khi gắn sai
    for fields in (("name",), ("name:vi", "official_name"), ("brand", "operator")):
        text = " ".join(tags.get(k, "") for k in fields).lower()
        for key, chain in CHAINS:
            if re.search(rf"\b{re.escape(key)}", text):
                return chain
    return None


FILLER = re.compile(r"\b(rạp|chiếu|phim|cinemas?|cineplex|star|theatre|rap|chieu)\b", re.IGNORECASE)


def is_generic(name: str, chain) -> bool:
    """Tên chỉ gồm tên hãng + từ chung chung, ví dụ 'rạp chiếu phim CGV', 'Lotte Cinema'."""
    if not chain:
        return False
    rest = name.lower()
    for key, c in CHAINS:
        if c == chain:
            rest = re.sub(rf"\b{re.escape(key)}\w*", " ", rest)
    rest = FILLER.sub(" ", rest)
    return not re.sub(r"[^\w]", "", rest)


def build_address(tags: dict):
    if tags.get("addr:full"):
        return tags["addr:full"]
    street = " ".join(p for p in (tags.get("addr:housenumber"), tags.get("addr:street")) if p)
    parts = [street] + [tags.get(k) for k in ("addr:subdistrict", "addr:district", "addr:city")]
    parts = [p for p in parts if p]
    return ", ".join(parts) if street else None


def fetch_raw() -> dict:
    return overpass_post(QUERY_FILE.read_text(encoding="utf-8"))


def overpass_post(query: str) -> dict:
    data = urllib.parse.urlencode({"data": query}).encode()
    for attempt, wait in enumerate((0, 15, 30, 60), start=1):
        time.sleep(wait)
        req = urllib.request.Request(OVERPASS_URL, data=data, headers={"User-Agent": "cinebook-dev-seed"})
        try:
            with urllib.request.urlopen(req, timeout=200) as resp:
                return json.loads(resp.read())
        except Exception as exc:  # server công cộng hay quá tải: thử lại với backoff
            print(f"Lần {attempt} thất bại: {exc}", file=sys.stderr)
    sys.exit("Overpass không phản hồi, thử lại sau.")


def distance_m(a, b) -> float:
    dlat = math.radians(b["lat"] - a["lat"])
    dlng = math.radians(b["lng"] - a["lng"]) * math.cos(math.radians(a["lat"]))
    return 6_371_000 * math.hypot(dlat, dlng)


def normalize(raw: dict) -> list:
    cinemas = []
    province = None
    for el in raw["elements"]:
        tags = el.get("tags", {})
        if el["type"] == "area":
            province = province_name(tags["name"])
            continue
        name = (tags.get("name:vi") or tags.get("name") or "").strip()
        if not name or EXCLUDE.search(name):
            continue
        chain = detect_chain(tags)
        center = el.get("center") or {"lat": el.get("lat"), "lon": el.get("lon")}
        item = {
            "osmId": f"{el['type']}/{el['id']}",
            "name": name,
            "chain": chain or INDEPENDENT,
            "provinceSlug": slugify(province),
            "provinceName": province,
            "lat": center["lat"],
            "lng": center["lon"],
            "address": build_address(tags),
            "phone": tags.get("phone") or tags.get("contact:phone"),
            "_generic": is_generic(name, chain),
            "_where": tags.get("addr:subdistrict") or tags.get("addr:district") or tags.get("addr:street"),
        }
        # Cùng một rạp vẽ 2 lần (node + building) -> bỏ bản sau
        if any(c["chain"] == item["chain"] and distance_m(c, item) < 150 for c in cinemas):
            continue
        cinemas.append(item)
    return cinemas


def enrich_with_ward(cinemas: list) -> None:
    """Hỏi OSM mỗi tọa độ thuộc phường/xã nào (1 request) cho rạp tên chung chung hoặc thiếu địa chỉ."""
    targets = [c for c in cinemas if c["_generic"] or not c["address"]]
    if not targets:
        return
    parts = ["[out:json][timeout:180];"]
    for i, c in enumerate(targets):
        parts.append(f'is_in({c["lat"]},{c["lng"]})->.a;'
                     f'area.a["boundary"="administrative"]["admin_level"~"^(6|7|8|9|10)$"];out tags;'
                     f'make marker idx={i};out;')
    result = overpass_post("\n".join(parts))
    best = {}
    pending = []
    for el in result["elements"]:
        if el["type"] == "marker":
            idx = int(el["tags"]["idx"])
            # Ưu tiên phường/xã; bỏ cấp quá chi tiết như "Khu phố", "Ấp", "Tổ dân phố"
            wards = [t for t in pending if re.match(r"^(Phường|Xã|Đặc khu|Thị trấn)\b", t["name"])]
            others = [t for t in pending if int(t.get("admin_level", 99)) <= 8]
            pick = wards or others
            if pick:
                best[idx] = max(pick, key=lambda t: int(t.get("admin_level", 0)))["name"]
            pending = []
        elif "name" in el.get("tags", {}):
            pending.append(el["tags"])
    for i, c in enumerate(targets):
        ward = best.get(i)
        if not ward:
            continue
        c["_where"] = c["_where"] or ward
        if not c["address"]:
            c["address"] = f"{ward}, {c['provinceName']}"


def finalize(cinemas: list) -> list:
    for c in cinemas:
        if c.pop("_generic"):
            c["name"] = f"{c['chain']} {c['_where'] or c['provinceName']}"
        c.pop("_where")
    # Tên còn trùng trong cùng tỉnh -> đánh số
    count = {}
    for c in cinemas:
        key = (slugify(c["name"]), c["provinceSlug"])
        count[key] = count.get(key, 0) + 1
        if count[key] > 1:
            c["name"] = f"{c['name']} {count[key]}"
    cinemas.sort(key=lambda c: (c["provinceSlug"], c["chain"], c["name"]))
    return cinemas


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--from-file", help="file JSON Overpass đã tải sẵn")
    ap.add_argument("--no-enrich", action="store_true", help="bỏ qua bước tra phường/xã")
    args = ap.parse_args()
    raw = json.loads(Path(args.from_file).read_text(encoding="utf-8")) if args.from_file else fetch_raw()
    cinemas = normalize(raw)
    if not args.no_enrich:
        enrich_with_ward(cinemas)
    cinemas = finalize(cinemas)
    out = {
        "source": "OpenStreetMap (Overpass API)",
        "license": "ODbL - © OpenStreetMap contributors",
        "fetchedAt": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "cinemas": cinemas,
    }
    OUT_FILE.write_text(json.dumps(out, ensure_ascii=False, indent=1), encoding="utf-8")
    chains = {}
    for c in cinemas:
        chains[c["chain"]] = chains.get(c["chain"], 0) + 1
    print(f"Đã ghi {len(cinemas)} rạp -> {OUT_FILE}")
    print(", ".join(f"{k}: {v}" for k, v in sorted(chains.items(), key=lambda x: -x[1])))


if __name__ == "__main__":
    main()
