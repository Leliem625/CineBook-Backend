#!/usr/bin/env python3
"""
Xây dựng danh sách rạp chiếu phim Việt Nam, gán chuẩn theo 34 tỉnh/thành (từ 01/07/2025).

Nguồn:
  - Overture Maps Places   (CDLA-Permissive-2.0 / Apache-2.0)  - nhiều rạp, nhưng nhiễu
  - OpenStreetMap/Overpass (ODbL, © OpenStreetMap contributors) - ít rạp hơn, tên/địa chỉ chuẩn hơn
  - Ranh giới tỉnh: Overture divisions (dữ liệu gốc từ OpenStreetMap, ODbL)

Quy trình: lọc rạp thật -> gộp bản ghi trùng giữa 2 nguồn -> gán tỉnh bằng point-in-polygon.

Cài đặt:  pip install duckdb
Chạy (từ thư mục cinebook-backend):
    python3 tools/places/build_cinemas.py              # dùng cache nếu có
    python3 tools/places/build_cinemas.py --refresh    # tải lại toàn bộ (Overture quét S3 ~5-10 phút)
Kết quả: src/main/resources/seed/cinemas-vn.json  (CinemaSeeder đọc file này)
"""
import argparse
import collections
import json
import math
import re
import sys
import time
import unicodedata
import urllib.parse
import urllib.request
from pathlib import Path

import duckdb

RELEASE = "2026-10-21.0"  # bản phát hành Overture: https://docs.overturemaps.org/release-calendar/
HERE = Path(__file__).resolve().parent
CACHE = HERE / ".cache"
OVERTURE_CACHE = CACHE / f"overture_cinema_candidates_{RELEASE}.parquet"
PROVINCE_CACHE = CACHE / f"vn_provinces_{RELEASE}.parquet"
OSM_CACHE = CACHE / "osm_cinemas.json"
OSM_QUERY = HERE / "cinemas_by_province.overpassql"
OUT_FILE = HERE.parent.parent / "src/main/resources/seed/cinemas-vn.json"
S3 = f"s3://overturemaps-us-west-2/release/{RELEASE}"

con = duckdb.connect()
for stmt in ("INSTALL spatial", "LOAD spatial", "INSTALL httpfs", "LOAD httpfs", "SET s3_region='us-west-2'"):
    con.execute(stmt)


# ====================================================================== tải dữ liệu (có cache)
def load_overture(refresh: bool) -> None:
    if refresh or not OVERTURE_CACHE.exists():
        print("Overture places: quét S3 (vài phút)...")
        t = time.time()
        con.execute(f"""
        COPY (
          SELECT id, names."primary" AS name, brand.names."primary" AS brand, taxonomy."primary" AS tax_primary,
                 confidence, ST_Y(geometry) AS lat, ST_X(geometry) AS lng,
                 addresses[1].freeform AS addr, phones[1] AS phone
          FROM read_parquet('{S3}/theme=places/type=place/*', hive_partitioning=1)
          WHERE bbox.xmin BETWEEN 102.1 AND 109.6 AND bbox.ymin BETWEEN 8.3 AND 23.5
            AND ( basic_category ILIKE '%movie%' OR basic_category ILIKE '%cinema%' OR basic_category ILIKE '%theat%'
               OR taxonomy."primary" ILIKE '%movie%' OR taxonomy."primary" ILIKE '%cinema%'
               OR regexp_matches(lower(names."primary"), 'cgv|lotte cinema|galaxy cine|bhd|beta cinema|cinestar|mega gs|'
                  || 'starlight|platinum cine|dcine|cinemax|rạp chiếu|rạp phim|cinema|cineplex') )
        ) TO '{OVERTURE_CACHE}' (FORMAT parquet)""")
        print(f"  xong sau {round(time.time() - t)} giây")
    con.execute(f"CREATE OR REPLACE TABLE cand AS SELECT * FROM read_parquet('{OVERTURE_CACHE}')")


def load_provinces(refresh: bool) -> None:
    if refresh or not PROVINCE_CACHE.exists():
        print("Overture divisions: tải ranh giới 34 tỉnh/thành (vài phút)...")
        con.execute(f"""
        COPY (
          SELECT names."primary" AS name, class, region, geometry
          FROM read_parquet('{S3}/theme=divisions/type=division_area/*', hive_partitioning=1)
          WHERE country = 'VN' AND subtype = 'region' AND class = 'land'
        ) TO '{PROVINCE_CACHE}' (FORMAT parquet)""")
    con.execute(f"CREATE OR REPLACE TABLE prov AS SELECT * FROM read_parquet('{PROVINCE_CACHE}')")
    n = con.execute("SELECT count(*) FROM prov").fetchone()[0]
    if n != 34:
        sys.exit(f"Ranh giới tỉnh không hợp lệ: {n} vùng (mong đợi 34)")


def load_osm(refresh: bool) -> None:
    if not refresh and OSM_CACHE.exists():
        return
    print("OpenStreetMap: gọi Overpass...")
    data = urllib.parse.urlencode({"data": OSM_QUERY.read_text(encoding="utf-8")}).encode()
    for attempt, wait in enumerate((0, 15, 30, 60), start=1):
        time.sleep(wait)
        req = urllib.request.Request("https://overpass-api.de/api/interpreter", data=data,
                                     headers={"User-Agent": "cinebook-dev-seed"})
        try:
            with urllib.request.urlopen(req, timeout=200) as resp:
                OSM_CACHE.write_bytes(resp.read())
                return
        except Exception as exc:  # server công cộng hay quá tải
            print(f"  lần {attempt} thất bại: {exc}", file=sys.stderr)
    sys.exit("Overpass không phản hồi, thử lại sau.")


# ====================================================================== quy tắc lọc / nhận diện
CHAINS = [  # (regex trên tên đã hạ chữ, tên chuẩn)
    (r"\bcgv\b", "CGV Cinemas"),
    (r"\blotte\b", "Lotte Cinema"),
    (r"\bgalaxy\s*(cine|cinema|nguyễn|nguyen|\w+ ?(plaza|mall))|^galaxy\b|rạp.*galaxy", "Galaxy Cinema"),
    (r"\bbhd\b", "BHD Star Cineplex"),
    (r"\bbeta\b", "Beta Cinemas"),
    (r"\bcinestar\b", "Cinestar"),
    (r"\bmega\s*gs\b", "Mega GS Cinemas"),
    (r"\bstarlight\b", "Starlight"),
    (r"\bplatinum\b", "Platinum Cineplex"),
    (r"\bdcine\b", "DCINE"),
    (r"\bcinemax\b", "Cinemax"),
    (r"\btouch cinema\b", "Touch Cinema"),
]
INDEPENDENT = "Rạp độc lập"
NOISE = re.compile(r"golf|drink|rượu|fishing|câu cá|telecom|viễn thông|office|văn phòng|tuyển dụng|online|homestay|hotel|khách sạn|"
                   r"coffee|cafe|café|cà phê|caphe|review|phim trường|studio|video|karaoke|nội thất|điện máy|inox|window film|"
                   r"box\b|cinebox|t-box|happy cinema|cơ giới|juice|sinh tố|bất động sản|in ấn|quảng cáo|camera|"
                   r"tổng tài|trọn bộ|shop|store|tiệm|ảnh màu|photo|trạm sạc|hải sản|chợ |nhà hàng|restaurant|đám cưới|phông rạp|mini", re.I)
CINEMA_WORD = re.compile(r"rạp|cinema|cineplex|chiếu phim|chiếu bóng|điện ảnh|movie|theater|theatre", re.I)
CHAIN_OK_CATS = {"movie_theater", "drive_in_theater", "arts_and_entertainment", "theatre_venue", None}


def slugify(t):
    t = t.replace("đ", "d").replace("Đ", "D")
    t = "".join(c for c in unicodedata.normalize("NFD", t) if unicodedata.category(c) != "Mn")
    return re.sub(r"[^a-z0-9]+", "-", t.lower()).strip("-")


def chain_of(name, brand=None):
    for text in (name, brand or ""):
        low = text.lower()
        for rx, chain in CHAINS:
            if re.search(rx, low):
                return chain
    return None


def dist(a, b):
    dlat = math.radians(b["lat"] - a["lat"])
    dlng = math.radians(b["lng"] - a["lng"]) * math.cos(math.radians(a["lat"]))
    return 6_371_000 * math.hypot(dlat, dlng)


def province_of(lat, lng):
    r = con.execute("SELECT name FROM prov WHERE class='land' AND ST_Contains(geometry, ST_Point(?, ?)) LIMIT 1",
                    [lng, lat]).fetchone()
    return re.sub(r"^(Thành phố|Tỉnh)\s+", "", r[0]) if r else None


GENERIC = re.compile(r"^(rạp chiếu phim |cụm rạp chiếu phim |rạp phim )?(cgv( cinemas?)?( vietnam| việt nam)?|lotte cinema|lotte|galaxy cinema|galaxy|"
                     r"bhd star( cineplex)?|beta cinemas?|cinestar|mega gs)$", re.I)


def is_generic(name):
    return bool(GENERIC.match(name.strip()))


def good_address(a):
    return bool(a) and bool(re.search(r"\d", a)) and len(a) > 12


# ====================================================================== lọc, gộp, gán tỉnh
def build() -> list:
    # Chỉ giữ địa điểm nằm trong lãnh thổ Việt Nam (đa giác 34 tỉnh)
    con.execute("""
    CREATE OR REPLACE TABLE vn AS
    SELECT c.* FROM cand c
    WHERE EXISTS (SELECT 1 FROM prov p WHERE ST_Contains(p.geometry, ST_Point(c.lng, c.lat)))""")
    # ---------------------------------------------------------------- Overture
    rows = con.execute("""
    SELECT id, trim(name), brand, tax_primary, confidence, lat, lng, addr, phone
    FROM vn WHERE name IS NOT NULL
    """).fetchall()
    ov = []
    for id_, name, brand, cat, conf, lat, lng, addr, phone in rows:
        if NOISE.search(name) or (re.search(r"lotte\s*mar|lotteria", name, re.I) and not re.search(r"cinema", name, re.I)):
            continue
        chain = chain_of(name, brand)
        if chain:
            if cat not in CHAIN_OK_CATS or conf < 0.3:
                continue
        else:
            if cat != "movie_theater" or conf < 0.5 or not CINEMA_WORD.search(name):
                continue
        ov.append(dict(source="overture", sourceId=id_, name=name, chain=chain or INDEPENDENT, conf=conf,
                       lat=lat, lng=lng, address=addr, phone=phone))

    # ---------------------------------------------------------------- OSM (đã lọc như fetch_cinemas.py)
    osm_raw = json.loads(OSM_CACHE.read_text(encoding="utf-8"))["elements"]
    OSM_EXCLUDE = re.compile(r"karaoke|tiệm ảnh|ảnh màu|studio ảnh", re.I)
    osm = []
    for el in osm_raw:
        if el["type"] == "area":
            continue
        t = el.get("tags", {})
        name = (t.get("name:vi") or t.get("name") or "").strip()
        if not name or OSM_EXCLUDE.search(name):
            continue
        c = el.get("center") or {"lat": el.get("lat"), "lon": el.get("lon")}
        street = " ".join(p for p in (t.get("addr:housenumber"), t.get("addr:street")) if p)
        addr = ", ".join(p for p in [street, t.get("addr:subdistrict"), t.get("addr:district"), t.get("addr:city")] if p) if street else None
        osm.append(dict(source="osm", sourceId=f"{el['type']}/{el['id']}", name=name,
                        chain=chain_of(name, t.get("brand")) or INDEPENDENT, conf=1.0,
                        lat=c["lat"], lng=c["lon"], address=addr, phone=t.get("phone")))

    # ---------------------------------------------------------------- Gộp: OSM trước (tên thường chuẩn hơn), rồi Overture theo độ tin cậy
    items = osm + sorted(ov, key=lambda x: -x["conf"])
    clusters = []
    for it in items:
        radius = 200 if it["chain"] != INDEPENDENT else 120
        match = next((c for c in clusters if c["chain"] == it["chain"] and dist(c, it) < radius), None)
        if match is None and it["chain"] != INDEPENDENT:
            # bản ghi rạp độc lập trùng vị trí với rạp chuỗi -> gộp vào rạp chuỗi
            match = next((c for c in clusters if c["chain"] == INDEPENDENT and dist(c, it) < 60), None)
            if match is not None:
                match["chain"] = it["chain"]
        if match is None:
            it["sources"] = [{"source": it["source"], "id": it["sourceId"]}]
            clusters.append(it)
            continue
        match["sources"].append({"source": it["source"], "id": it["sourceId"]})
        if is_generic(match["name"]) and not is_generic(it["name"]):
            match["name"] = it["name"]
        if not good_address(match["address"]) and good_address(it["address"]):
            match["address"] = it["address"]
        match["phone"] = match["phone"] or it["phone"]

    # ---------------------------------------------------------------- Gộp lần 2: tên giống nhau / trùng địa chỉ
    STOP = set("cgv cinemas cinema lotte galaxy bhd star cineplex beta cinestar mega gs rap chieu phim cum tai tang tttm "
               "viet vietnam the".split())


    def tokens(name):
        return {t for t in slugify(name).split("-") if t and t not in STOP}


    def addr_key(a):
        m = re.search(r"(\d+[a-z]?(?:-\d+)?)\s+(?:đường|đ\.|phố|p\.)?\s*([^,]+)", (a or "").lower())
        return f"{m.group(1)}|{slugify(m.group(2))[:20]}" if m else None


    def same(a, b):
        if a["chain"] != b["chain"] or a["chain"] == INDEPENDENT or dist(a, b) > 5000:
            return False
        ta, tb = tokens(a["name"]), tokens(b["name"])
        if ta and tb and len(ta & tb) / len(ta | tb) >= 0.5 and (len(ta & tb) >= 2 or dist(a, b) < 2000):
            return True
        ka, kb = addr_key(a["address"]), addr_key(b["address"])
        if ka and ka == kb:
            return True
        # bản ghi tên chung chung, chỉ có từ Overture, độ tin cậy thấp -> gộp vào rạp cùng hãng gần nhất (< 1 km)
        for x, y in ((a, b), (b, a)):
            if is_generic(x["name"]) and {s["source"] for s in x["sources"]} == {"overture"} and dist(x, y) < 1000:
                return True
        return False


    def street_hint(address):
        for part in (address or "").split(","):
            m = re.match(r"^\s*[\d/A-Za-z-]*\d[\d/A-Za-z-]*\s+(?:Đường|Đ\.|Phố|P\.)?\s*(.+)$", part)
            if m:
                return m.group(1).strip()
        return None


    for c in clusters:
        c["prov"] = province_of(c["lat"], c["lng"])
        # Đặt tên cụ thể cho rạp tên chung chung TRƯỚC khi gộp, để so tên được
        if is_generic(c["name"]):
            if {s["source"] for s in c["sources"]} == {"overture"} and not good_address(c["address"]):
                c["_unverifiable"] = True
            c["name"] = f"{c['chain']} {street_hint(c['address']) or c['prov'] or ''}".strip()
    STOP |= set("quan q tp hcm ho chi minh ha noi thanh pho cinema".split())


    def same_name_in_province(a, b):
        if a["chain"] != b["chain"] or a["chain"] == INDEPENDENT or not a["prov"] or a["prov"] != b["prov"]:
            return False
        ta, tb = tokens(a["name"]), tokens(b["name"])
        inter = ta & tb
        return len(inter) >= 2 and len(inter) / min(len(ta), len(tb)) >= 0.8 and dist(a, b) < 30000


    parent = list(range(len(clusters)))
    def find(i):
        while parent[i] != i:
            parent[i] = parent[parent[i]]
            i = parent[i]
        return i
    for i in range(len(clusters)):
        for j in range(i + 1, len(clusters)):
            if same(clusters[i], clusters[j]) or same_name_in_province(clusters[i], clusters[j]):
                parent[find(j)] = find(i)
    groups = collections.defaultdict(list)
    for i, c in enumerate(clusters):
        groups[find(i)].append(c)
    merged = []
    for g in groups.values():
        # ưu tiên bản có nguồn OSM, rồi tên cụ thể, rồi độ tin cậy cao
        g.sort(key=lambda c: (c.get("_unverifiable", False), not any(s["source"] == "osm" for s in c["sources"]), -c["conf"]))
        head = dict(g[0])
        for other in g[1:]:
            head["sources"] = head["sources"] + other["sources"]
            if not good_address(head["address"]) and good_address(other["address"]):
                head["address"] = other["address"]
            head["phone"] = head["phone"] or other["phone"]
        merged.append(head)
    clusters = merged

    # ---------------------------------------------------------------- Gán tỉnh, chuẩn hóa tên
    result = []
    for c in clusters:
        prov = c["prov"]
        if not prov:
            continue
        if c.get("_unverifiable"):
            continue  # tên chung chung, không địa chỉ, chỉ 1 nguồn -> không kiểm chứng được
        name = c["name"]
        name = re.sub(r"^(Rạp chiếu phim|Cụm Rạp Chiếu Phim|Rạp Phim)\s+(?=(CGV|Lotte|LOTTE|Galaxy|BHD|Beta|Cinestar|Mega))", "", name, flags=re.I)
        result.append(dict(sourceId=f"{c['source']}:{c['sourceId']}", name=name.strip(), chain=c["chain"],
                           provinceSlug=slugify(prov), provinceName=prov, lat=round(c["lat"], 6), lng=round(c["lng"], 6),
                           address=c["address"] or f"{prov}", phone=c["phone"],
                           sources=c["sources"]))

    # Tên trùng trong tỉnh -> đánh số
    cnt = collections.Counter()
    for r in sorted(result, key=lambda r: r["sourceId"]):
        k = (slugify(r["name"]), r["provinceSlug"])
        cnt[k] += 1
        if cnt[k] > 1:
            r["name"] = f"{r['name']} {cnt[k]}"
    result.sort(key=lambda r: (r["provinceSlug"], r["chain"], r["name"]))
    return result


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--refresh", action="store_true", help="tải lại dữ liệu nguồn, bỏ qua cache")
    args = ap.parse_args()
    CACHE.mkdir(exist_ok=True)
    load_provinces(args.refresh)
    load_overture(args.refresh)
    load_osm(args.refresh)
    result = build()
    OUT_FILE.write_text(json.dumps({
        "sources": ["OpenStreetMap (ODbL, © OpenStreetMap contributors)",
                    f"Overture Maps Places {RELEASE} (CDLA-Permissive-2.0 / Apache-2.0)"],
        "boundaries": f"Overture divisions {RELEASE} (dữ liệu gốc OpenStreetMap, ODbL) - 34 tỉnh/thành",
        "cinemas": result,
    }, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"Đã ghi {len(result)} rạp -> {OUT_FILE}")
    print("Theo hãng:", ", ".join(f"{k} {v}" for k, v in collections.Counter(r["chain"] for r in result).most_common()))
    print("Số tỉnh có rạp:", len({r["provinceSlug"] for r in result}), "/ 34")


if __name__ == "__main__":
    main()
