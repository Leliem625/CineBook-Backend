# -*- coding: utf-8 -*-
"""Sinh tài liệu SRS CineBook ra PDF (reportlab + font Arial hỗ trợ tiếng Việt)."""
import sys
from reportlab.lib.pagesizes import A4
from reportlab.lib.units import mm
from reportlab.lib import colors
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.enums import TA_CENTER
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (BaseDocTemplate, PageTemplate, Frame, Paragraph, Spacer,
                                Table, TableStyle, PageBreak, Preformatted, KeepTogether)
from reportlab.platypus.tableofcontents import TableOfContents

OUT = sys.argv[1]
FD = "/System/Library/Fonts/Supplemental/"
pdfmetrics.registerFont(TTFont("Arial", FD + "Arial.ttf"))
pdfmetrics.registerFont(TTFont("Arial-Bold", FD + "Arial Bold.ttf"))
pdfmetrics.registerFont(TTFont("Arial-Italic", FD + "Arial Italic.ttf"))
pdfmetrics.registerFont(TTFont("Arial-BoldItalic", FD + "Arial Bold Italic.ttf"))
pdfmetrics.registerFont(TTFont("Mono", FD + "Courier New.ttf"))
from reportlab.pdfbase.pdfmetrics import registerFontFamily
registerFontFamily("Arial", normal="Arial", bold="Arial-Bold", italic="Arial-Italic", boldItalic="Arial-BoldItalic")

ACCENT = colors.HexColor("#B4232C")
DARK = colors.HexColor("#1F2328")
MUTED = colors.HexColor("#5B6470")
LIGHT = colors.HexColor("#F4F5F7")
LINE = colors.HexColor("#D0D5DB")

S = {
    "title": ParagraphStyle("title", fontName="Arial-Bold", fontSize=26, leading=32, textColor=DARK, alignment=TA_CENTER),
    "subtitle": ParagraphStyle("subtitle", fontName="Arial", fontSize=13, leading=18, textColor=MUTED, alignment=TA_CENTER),
    "h1": ParagraphStyle("h1", fontName="Arial-Bold", fontSize=17, leading=22, textColor=ACCENT, spaceBefore=6, spaceAfter=8),
    "h2": ParagraphStyle("h2", fontName="Arial-Bold", fontSize=12.5, leading=17, textColor=DARK, spaceBefore=10, spaceAfter=5, keepWithNext=1),
    "h3": ParagraphStyle("h3", fontName="Arial-Bold", fontSize=10.5, leading=14, textColor=DARK, spaceBefore=6, spaceAfter=3, keepWithNext=1),
    "p": ParagraphStyle("p", fontName="Arial", fontSize=9.5, leading=14, textColor=DARK, spaceAfter=5),
    "li": ParagraphStyle("li", fontName="Arial", fontSize=9.5, leading=13.5, textColor=DARK, leftIndent=14, bulletIndent=4, spaceAfter=2),
    "cell": ParagraphStyle("cell", fontName="Arial", fontSize=8.5, leading=11.5, textColor=DARK),
    "cellh": ParagraphStyle("cellh", fontName="Arial-Bold", fontSize=8.5, leading=11.5, textColor=colors.white),
    "code": ParagraphStyle("code", fontName="Mono", fontSize=7.8, leading=10, textColor=DARK),
    "note": ParagraphStyle("note", fontName="Arial-Italic", fontSize=9, leading=13, textColor=MUTED, spaceAfter=5),
    "toc1": ParagraphStyle("toc1", fontName="Arial-Bold", fontSize=10, leading=16, leftIndent=0),
    "toc2": ParagraphStyle("toc2", fontName="Arial", fontSize=9, leading=13, leftIndent=16, textColor=MUTED),
}


class Doc(BaseDocTemplate):
    def __init__(self, fn):
        super().__init__(fn, pagesize=A4, leftMargin=20 * mm, rightMargin=20 * mm, topMargin=20 * mm, bottomMargin=18 * mm,
                         title="CineBook - Tài liệu đặc tả yêu cầu phần mềm (SRS)", author="CineBook project",
                         subject="Software Requirements Specification")
        frame = Frame(self.leftMargin, self.bottomMargin, self.width, self.height, id="f")
        self.addPageTemplates([PageTemplate(id="cover", frames=[frame]),
                               PageTemplate(id="body", frames=[frame], onPage=self._decor)])

    def _decor(self, c, doc):
        c.saveState()
        c.setFont("Arial", 8)
        c.setFillColor(MUTED)
        c.drawString(20 * mm, A4[1] - 12 * mm, "CineBook - Đặc tả yêu cầu phần mềm (SRS) v1.1")
        c.drawRightString(A4[0] - 20 * mm, 10 * mm, f"Trang {doc.page}")
        c.setStrokeColor(LINE)
        c.line(20 * mm, A4[1] - 13.5 * mm, A4[0] - 20 * mm, A4[1] - 13.5 * mm)
        c.restoreState()

    def afterFlowable(self, f):
        if isinstance(f, Paragraph) and f.style.name in ("h1", "h2"):
            lvl = 0 if f.style.name == "h1" else 1
            key = "k%d" % id(f)
            self.canv.bookmarkPage(key)
            self.canv.addOutlineEntry(f.getPlainText(), key, level=lvl, closed=False)
            self.notify("TOCEntry", (lvl, f.getPlainText(), self.page, key))


story = []
def H1(t, new_page=True):
    if new_page:
        story.append(PageBreak())
    story.append(Paragraph(t, S["h1"]))
def H2(t): story.append(Paragraph(t, S["h2"]))
def H3(t): story.append(Paragraph(t, S["h3"]))
def P(t): story.append(Paragraph(t, S["p"]))
def NOTE(t): story.append(Paragraph(t, S["note"]))
def UL(items):
    for it in items:
        story.append(Paragraph(it, S["li"], bulletText="•"))
    story.append(Spacer(1, 3))
def CODE(t):
    pre = Preformatted(t.strip("\n"), S["code"])
    tb = Table([[pre]], colWidths=[170 * mm])
    tb.setStyle(TableStyle([("BACKGROUND", (0, 0), (-1, -1), LIGHT), ("BOX", (0, 0), (-1, -1), 0.5, LINE),
                            ("LEFTPADDING", (0, 0), (-1, -1), 6), ("TOPPADDING", (0, 0), (-1, -1), 5),
                            ("BOTTOMPADDING", (0, 0), (-1, -1), 5)]))
    story.append(tb)
    story.append(Spacer(1, 6))
def TABLE(header, rows, widths):
    total = 170.0
    ws = [w / sum(widths) * total * mm for w in widths]
    data = [[Paragraph(h, S["cellh"]) for h in header]]
    for r in rows:
        data.append([Paragraph(str(c), S["cell"]) for c in r])
    tb = Table(data, colWidths=ws, repeatRows=1)
    st = [("BACKGROUND", (0, 0), (-1, 0), DARK), ("VALIGN", (0, 0), (-1, -1), "TOP"),
          ("GRID", (0, 0), (-1, -1), 0.4, LINE), ("LEFTPADDING", (0, 0), (-1, -1), 4),
          ("RIGHTPADDING", (0, 0), (-1, -1), 4), ("TOPPADDING", (0, 0), (-1, -1), 3),
          ("BOTTOMPADDING", (0, 0), (-1, -1), 3)]
    for i in range(1, len(data)):
        if i % 2 == 0:
            st.append(("BACKGROUND", (0, i), (-1, i), LIGHT))
    tb.setStyle(TableStyle(st))
    story.append(tb)
    story.append(Spacer(1, 8))
def FR(rows):
    TABLE(["Mã", "Yêu cầu", "Ưu tiên"], rows, [11, 80, 12])


# ---------------------------------------------------------------- COVER
story.append(Spacer(1, 55 * mm))
story.append(Paragraph("CineBook", S["title"]))
story.append(Spacer(1, 6))
story.append(Paragraph("Nền tảng lịch chiếu, đặt vé xem phim và cộng đồng điện ảnh", S["subtitle"]))
story.append(Spacer(1, 14 * mm))
story.append(Paragraph("TÀI LIỆU ĐẶC TẢ YÊU CẦU PHẦN MỀM (SRS)", ParagraphStyle("x", parent=S["subtitle"], fontName="Arial-Bold", textColor=ACCENT)))
story.append(Spacer(1, 30 * mm))
cover = [["Phiên bản", "1.1 (đặt vé không cần tài khoản, lược bỏ phần không trọng tâm)"], ["Ngày", "12/11/2026"], ["Loại dự án", "Dự án cá nhân, phi thương mại (học tập và portfolio)"],
         ["Frontend", "Next.js (React, TypeScript)"], ["Backend", "Java 21, Spring Boot 3"],
         ["Hạ tầng", "PostgreSQL, Redis, Apache Kafka, Docker, Kubernetes"]]
ct = Table([[Paragraph("<b>%s</b>" % a, S["cell"]), Paragraph(b, S["cell"])] for a, b in cover], colWidths=[40 * mm, 100 * mm])
ct.setStyle(TableStyle([("GRID", (0, 0), (-1, -1), 0.4, LINE), ("BACKGROUND", (0, 0), (0, -1), LIGHT),
                        ("TOPPADDING", (0, 0), (-1, -1), 4), ("BOTTOMPADDING", (0, 0), (-1, -1), 4)]))
story.append(ct)

# ---------------------------------------------------------------- TOC
from reportlab.platypus.doctemplate import NextPageTemplate
story.append(NextPageTemplate("body"))
story.append(PageBreak())
story.append(Paragraph("Mục lục", ParagraphStyle("tochead", parent=S["h1"])))
toc = TableOfContents()
toc.levelStyles = [S["toc1"], S["toc2"]]
story.append(toc)


# ================================================================ v1.1 BODY
# ---------------------------------------------------------------- 1
H1("1. Giới thiệu")
H2("1.1. Mục đích tài liệu")
P("Tài liệu đặc tả yêu cầu chức năng, quy tắc nghiệp vụ, dữ liệu, kiến trúc và yêu cầu phi chức năng của <b>CineBook</b>: "
  "website lịch chiếu và đặt vé xem phim. Dùng làm cơ sở để thiết kế, lập trình, kiểm thử và nghiệm thu.")
H2("1.2. Mục tiêu dự án")
P("Dự án cá nhân với mục tiêu chính là <b>nâng cao kỹ năng system design</b>. Vì vậy phạm vi được chọn theo nguyên tắc: "
  "<b>giữ lại tính năng tạo ra bài toán hệ thống thú vị, lược bỏ tính năng chỉ là CRUD hoặc nội dung</b>. Các bài toán trọng tâm:")
UL(["<b>Concurrency và transaction:</b> không bán trùng ghế khi hàng nghìn người cùng đặt; voucher không vượt số lượt.",
    "<b>Kiến trúc hướng sự kiện:</b> Apache Kafka, Transactional Outbox, consumer idempotent, Saga, dead letter.",
    "<b>Tích hợp bên thứ ba không tin cậy:</b> cổng thanh toán (IPN trùng, đến muộn, sai lệch), TMDB API (timeout, rate limit).",
    "<b>Hiệu năng và mở rộng:</b> cache nhiều tầng, phòng chờ ảo khi mở bán, rate limiting, WebSocket nhiều pod.",
    "<b>AI:</b> trợ lý tìm suất chiếu và gợi ý ghế bằng LLM tool calling an toàn.",
    "<b>Vận hành:</b> Kubernetes, CI/CD, giám sát, load test, chaos test."])
H2("1.3. Phạm vi")
TABLE(["Trong phạm vi", "Lược bỏ (lý do: ít giá trị system design)"], [
    ["Danh sách phim, chi tiết phim (dữ liệu TMDB, gồm tên đạo diễn, diễn viên dạng text)", "Trang nghệ sĩ riêng"],
    ["Lịch chiếu theo khu vực, ngày, rạp, định dạng", "Rạp gần tôi, rạp yêu thích"],
    ["Hệ thống rạp, cụm rạp, phòng, sơ đồ ghế", "Tin tức, bài viết, video, chuyên đề, CMS nội dung"],
    ["Đặt vé không cần tài khoản: giữ ghế, combo, voucher, thanh toán, vé QR qua email", "Cộng đồng: chấm điểm, review, bình luận, thích, watchlist, trang cá nhân (điểm phim lấy từ TMDB)"],
    ["Tra cứu vé, hủy vé có xác thực OTP qua email", "Ví điểm thưởng, thông báo trong web, tải lịch .ics"],
    ["Soát vé QR, trang quản trị, dashboard, bảng xếp hạng realtime", "Tìm kiếm chuyên dụng (Meilisearch): thay bằng PostgreSQL unaccent + pg_trgm"],
    ["Trợ lý AI (tìm suất, gợi ý ghế, hỏi đáp chính sách)", "AI tóm tắt review, gợi ý cá nhân hóa, kiểm duyệt nội dung"],
    ["Tài khoản khách hàng: <b>làm cuối cùng</b>, tùy chọn (mục 4.10)", ""],
], [50, 50])
H2("1.4. Tham khảo")
UL(["Mô hình chức năng tham khảo: các website lịch chiếu và đặt vé tại Việt Nam (ví dụ moveek.com). CineBook là sản phẩm độc lập, "
    "không sao chép giao diện, thương hiệu hay dữ liệu của website khác.",
    "TMDB API (dữ liệu phim): https://developer.themoviedb.org",
    "VNPay Sandbox (thanh toán thử nghiệm): https://sandbox.vnpayment.vn"])
H2("1.5. Thuật ngữ")
TABLE(["Thuật ngữ", "Giải thích"], [
    ["Hệ thống rạp (Chain)", "Thương hiệu rạp, sở hữu nhiều cụm rạp. Trong CineBook là các thương hiệu hư cấu."],
    ["Cụm rạp (Cinema)", "Một địa điểm rạp cụ thể, có địa chỉ, gồm nhiều phòng chiếu."],
    ["Phòng chiếu (Room)", "Phòng có sơ đồ ghế cố định và các định dạng hỗ trợ (2D, 3D, IMAX...)."],
    ["Suất chiếu (Showtime)", "Một lần chiếu phim tại một phòng, vào một thời điểm, theo một định dạng và ngôn ngữ."],
    ["Ghế theo suất (Showtime seat)", "Trạng thái của từng ghế cho từng suất chiếu."],
    ["Giữ ghế (Hold)", "Tạm khóa ghế trong thời gian giới hạn để khách nhập thông tin và thanh toán."],
    ["Phiên khách (Guest session)", "Định danh ẩn danh (cookie ký số) gắn với trình duyệt, dùng thay cho tài khoản khi giữ ghế."],
    ["Booking", "Đơn đặt vé: ghế, combo, voucher, thông tin liên hệ, thanh toán."],
    ["IPN", "Instant Payment Notification: cổng thanh toán gọi về server để báo kết quả."],
    ["Outbox", "Bảng lưu sự kiện trong cùng transaction với dữ liệu nghiệp vụ, sau đó được đẩy lên Kafka."],
    ["Idempotent", "Thực hiện một thao tác nhiều lần cho kết quả giống như thực hiện một lần."],
    ["Saga", "Chuỗi bước giao dịch phân tán, mỗi bước có hành động bù trừ khi thất bại."],
    ["Phòng chờ ảo (Waiting room)", "Hàng đợi chặn trước trang đặt vé khi lượng truy cập vượt ngưỡng, cấp lượt vào theo thứ tự."],
], [30, 70])

# ---------------------------------------------------------------- 2
H1("2. Mô tả tổng quan")
H2("2.1. Bối cảnh sản phẩm")
P("CineBook vừa hiển thị lịch chiếu của nhiều hệ thống rạp tại nhiều tỉnh/thành, vừa là hệ thống bán vé trực tiếp (quản lý ghế, suất chiếu, "
  "thanh toán). Hệ thống rạp, cụm rạp và suất chiếu là dữ liệu hư cấu do CineBook tự sinh; dữ liệu phim lấy từ TMDB.")
H2("2.2. Vai trò người dùng")
TABLE(["Vai trò", "Đăng nhập", "Quyền chính"], [
    ["Khách hàng", "Không cần", "Xem phim, lịch chiếu, rạp; đặt vé và thanh toán bằng tên, email, SĐT; tra cứu và hủy vé qua mã đặt vé + OTP email; dùng trợ lý AI"],
    ["Nhân viên rạp (Staff)", "Có", "Quét QR soát vé tại cụm rạp của mình"],
    ["Quản lý rạp (Manager)", "Có", "Quản lý phòng, suất chiếu, giá, combo của hệ thống rạp mình; xem báo cáo"],
    ["Quản trị viên (Admin)", "Có", "Toàn quyền: phim, hệ thống rạp, voucher, tài khoản nội bộ, cấu hình, báo cáo"],
], [20, 13, 67])
NOTE("Đăng nhập nội bộ (Staff/Manager/Admin) ở giai đoạn đầu làm đơn giản: tài khoản seed sẵn, form login của Spring Security. "
     "Hoàn thiện xác thực (JWT, quên mật khẩu) ở giai đoạn cuối cùng với tài khoản khách hàng.")
H2("2.3. Ràng buộc")
UL(["Dự án <b>phi thương mại</b>. Thanh toán chỉ dùng <b>sandbox</b> (VNPay sandbox; tùy chọn MoMo test, Stripe test mode). Banner "
    "\"Dự án demo - không bán vé thật\" trên mọi trang.",
    "Không dùng tên, logo, dữ liệu của hệ thống rạp có thật; không crawl website khác. Dữ liệu phim lấy qua TMDB API, hiển thị ghi nhận nguồn TMDB ở footer.",
    "Giao diện tiếng Việt, múi giờ Asia/Ho_Chi_Minh, tiền tệ VND.",
    "Công nghệ: Next.js, Spring Boot, PostgreSQL, Redis, Kafka; triển khai cuối cùng trên Kubernetes."])
H2("2.4. Giả định")
UL(["Khách hàng nhập email đúng để nhận vé; hệ thống không xác thực email trước khi thanh toán.",
    "Có tài khoản gửi email (Mailpit khi local; Resend/Mailtrap khi triển khai) và API key LLM."])

# ---------------------------------------------------------------- 3
H1("3. Sơ đồ trang (Sitemap) và URL")
P("URL dùng slug tiếng Việt không dấu. Trang công khai render phía server (SSR/ISR) bằng Next.js để tối ưu SEO và cache.")
TABLE(["Trang", "URL", "Nội dung chính"], [
    ["Trang chủ", "/", "Banner, tab Đang chiếu / Sắp chiếu, top phim bán chạy (realtime), mua vé nhanh"],
    ["Mua vé nhanh", "/mua-ve", "Chọn phim → khu vực → ngày → rạp → suất"],
    ["Lịch chiếu", "/lich-chieu", "Lọc theo khu vực, ngày, hệ thống rạp, cụm rạp, phim, định dạng"],
    ["Danh sách phim", "/dang-chieu, /sap-chieu", "Lưới phim có lọc thể loại, sắp xếp"],
    ["Chi tiết phim", "/phim/{slug}", "Thông tin, trailer, điểm TMDB, lịch chiếu"],
    ["Hệ thống rạp", "/rap, /he-thong-rap/{slug}", "Danh sách hệ thống rạp và cụm rạp theo tỉnh"],
    ["Cụm rạp", "/rap/{slug}", "Địa chỉ, bản đồ, bảng giá, lịch chiếu theo ngày"],
    ["Phòng chờ", "/hang-cho/{showtimeId}", "Hiển thị vị trí trong hàng đợi khi suất chiếu đang quá tải"],
    ["Đặt vé", "/dat-ve/{showtimeId}", "Bước 1 chọn ghế → bước 2 combo → bước 3 thông tin liên hệ + voucher → bước 4 thanh toán"],
    ["Kết quả thanh toán", "/dat-ve/ket-qua?code=", "Trạng thái booking sau khi quay về từ cổng thanh toán; hiển thị vé"],
    ["Tra cứu vé", "/tra-cuu-ve", "Nhập mã đặt vé + email/SĐT → xem vé, hủy vé"],
    ["Vé điện tử", "/ve/{token}", "Link trong email (token ký số): QR, thông tin vé"],
    ["Bảng xếp hạng", "/bang-xep-hang", "Top phim theo vé bán hôm nay / tuần (cập nhật realtime)"],
    ["Tìm kiếm", "/tim-kiem?q=", "Phim và rạp"],
    ["Hỗ trợ", "/ho-tro, /chinh-sach", "FAQ, chính sách hủy vé, điều khoản"],
    ["Quản trị", "/admin/...", "Mục 4.9"],
    ["Soát vé", "/staff/check-in", "Quét QR (tối ưu mobile)"],
], [18, 30, 52])

# ---------------------------------------------------------------- 4
H1("4. Yêu cầu chức năng")
P("Mức ưu tiên: <b>P0</b> = bắt buộc cho MVP, <b>P1</b> = cần cho bản hoàn chỉnh, <b>P2</b> = nâng cao / làm sau.")

H2("4.1. M01 - Phim")
FR([
    ["FR-101", "Danh sách phim đang chiếu và sắp chiếu; lọc thể loại, nhãn độ tuổi; sắp xếp theo ngày khởi chiếu, độ phổ biến, số vé bán.", "P0"],
    ["FR-102", "Thẻ phim: poster, tên, ngày khởi chiếu, nhãn độ tuổi, điểm TMDB, nút Mua vé (nếu còn suất).", "P0"],
    ["FR-103", "Chi tiết phim: tên Việt, tên gốc, thể loại, thời lượng, khởi chiếu, nhãn độ tuổi (P, K, T13, T16, T18), quốc gia, đạo diễn và diễn viên (chỉ tên), tóm tắt, poster, backdrop, trailer (YouTube embed).", "P0"],
    ["FR-104", "Widget lịch chiếu trên trang phim: chọn tỉnh/thành, dải 7 ngày, định dạng (2D, 3D, IMAX), ngôn ngữ (phụ đề, lồng tiếng); nhóm theo hệ thống rạp → cụm rạp.", "P0"],
    ["FR-105", "Đồng bộ phim tự động từ TMDB (mục 7.3); admin có thể sửa đè thông tin.", "P0"],
])
H2("4.2. M02 - Rạp và lịch chiếu")
FR([
    ["FR-201", "Danh sách hệ thống rạp (logo, số cụm rạp); trang hệ thống rạp liệt kê cụm rạp, lọc theo tỉnh.", "P0"],
    ["FR-202", "Trang cụm rạp: địa chỉ, bản đồ (Leaflet + OpenStreetMap), bảng giá, lịch chiếu theo ngày nhóm theo phim.", "P0"],
    ["FR-203", "Trang lịch chiếu: khu vực → ngày → hệ thống rạp → cụm rạp; lọc phim, định dạng, ngôn ngữ, khung giờ.", "P0"],
    ["FR-204", "Mỗi giờ chiếu hiển thị mức còn ghế: còn nhiều / sắp hết / hết vé / ngừng bán (số liệu cache, trễ tối đa 30 giây).", "P1"],
    ["FR-205", "Ghi nhớ khu vực đã chọn bằng cookie.", "P1"],
])
H2("4.3. M03 - Đặt vé (không cần tài khoản)")
FR([
    ["FR-301", "Bấm giờ chiếu → trang đặt vé. Hệ thống cấp <b>phiên khách</b> (cookie ký số, httpOnly) nếu chưa có.", "P0"],
    ["FR-302", "Sơ đồ ghế: màn hình, lối đi, loại ghế (thường, VIP, đôi) với màu và giá; trạng thái trống / đang chọn / đã giữ / đã bán / không bán.", "P0"],
    ["FR-303", "Cập nhật trạng thái ghế realtime qua WebSocket khi người khác giữ hoặc nhả ghế.", "P0"],
    ["FR-304", "Chọn ghế → Tiếp tục → API giữ ghế (nguyên tử, theo BR-01..10). Thất bại thì báo ghế nào không còn và làm mới sơ đồ.", "P0"],
    ["FR-305", "Đồng hồ đếm ngược thời gian giữ ghế trên mọi bước; hết giờ thì thông báo và quay về sơ đồ ghế.", "P0"],
    ["FR-306", "Chọn combo bắp nước (0-10 mỗi loại) theo danh mục của hệ thống rạp.", "P1"],
    ["FR-307", "Form thông tin liên hệ: <b>họ tên</b> (2-100 ký tự), <b>email</b> (định dạng hợp lệ), <b>số điện thoại</b> (định dạng VN, chuẩn hóa về +84). Nhập email 2 lần hoặc hiển thị lại để khách kiểm tra.", "P0"],
    ["FR-308", "Nhập mã voucher, hiển thị số tiền giảm.", "P1"],
    ["FR-309", "Trang xác nhận: phim, rạp, phòng, giờ, ghế, combo, thông tin liên hệ, tạm tính, giảm giá, tổng tiền; đồng ý điều khoản; chọn phương thức thanh toán.", "P0"],
    ["FR-310", "<b>Thanh toán trước khi xuất vé</b>: chuyển sang cổng sandbox; quay về trang kết quả hiển thị trạng thái (đang xử lý / thành công / thất bại), tự cập nhật khi IPN đến.", "P0"],
    ["FR-311", "Thành công: hiển thị vé QR và mã đặt vé (8 ký tự, không nhập nhằng như 0/O, 1/I); gửi email vé có link /ve/{token}.", "P0"],
    ["FR-312", "Khách có thể hủy giữ ghế chủ động trước khi thanh toán.", "P0"],
    ["FR-313", "Chống bot: Cloudflare Turnstile (CAPTCHA) ở bước giữ ghế khi suất chiếu đang ở chế độ cao tải hoặc phiên có hành vi bất thường.", "P1"],
])
H2("4.4. M04 - Tra cứu và hủy vé")
FR([
    ["FR-401", "Tra cứu vé bằng mã đặt vé + email hoặc SĐT; giới hạn 5 lần sai mỗi 15 phút theo IP.", "P0"],
    ["FR-402", "Gửi lại email vé (tối đa 3 lần mỗi booking).", "P1"],
    ["FR-403", "Hủy vé: khách yêu cầu hủy → hệ thống gửi OTP 6 số qua email (hiệu lực 5 phút, tối đa 5 lần nhập) → xác nhận → hoàn tiền theo BR-30.", "P1"],
    ["FR-404", "Link vé trong email dùng token ký số, không đoán được; không hiển thị đầy đủ email/SĐT (che một phần).", "P0"],
])
H2("4.5. M05 - Thanh toán")
FR([
    ["FR-501", "Tích hợp VNPay sandbox: tạo URL thanh toán có chữ ký HMAC-SHA512; xử lý return URL và IPN.", "P0"],
    ["FR-502", "Xử lý IPN idempotent; kiểm tra chữ ký, số tiền, mã giao dịch (BR-20..25).", "P0"],
    ["FR-503", "Lưu mọi lần thanh toán và mọi callback nhận được (raw payload) để đối soát.", "P0"],
    ["FR-504", "Hoàn tiền qua API sandbox hoặc mô phỏng; theo dõi trạng thái hoàn tiền; retry khi lỗi.", "P1"],
    ["FR-505", "Job đối soát: truy vấn cổng thanh toán cho các payment PENDING quá 15 phút (phòng khi mất IPN).", "P1"],
])
H2("4.6. M06 - Thông báo email")
FR([
    ["FR-601", "Email giao dịch: vé điện tử, OTP hủy vé, xác nhận hủy/hoàn tiền, suất chiếu bị hủy, thanh toán muộn được hoàn tiền.", "P0"],
    ["FR-602", "Gửi email bất đồng bộ qua Kafka consumer; retry có backoff; không gửi trùng (idempotent theo event_id).", "P0"],
    ["FR-603", "Email nhắc lịch chiếu trước 2 giờ (delayed job).", "P2"],
])
H2("4.7. M07 - Trợ lý AI")
FR([
    ["FR-701", "Khung chat nổi; trả lời tiếng Việt, streaming (Server-Sent Events).", "P1"],
    ["FR-702", "Tìm phim và suất chiếu bằng ngôn ngữ tự nhiên (\"tối thứ 7 có phim hành động nào ở Quận 1\").", "P1"],
    ["FR-703", "Gợi ý ghế đẹp (giữa phòng, liền nhau) cho số người yêu cầu; trả về nút mở trang đặt vé với ghế chọn sẵn.", "P1"],
    ["FR-704", "Hỏi đáp chính sách (hủy vé, giá, độ tuổi) dựa trên tài liệu nội bộ (RAG, pgvector).", "P2"],
    ["FR-705", "AI chỉ có công cụ đọc; không tự giữ ghế, thanh toán hay hủy vé (BR-60).", "P0"],
])
H2("4.8. M08 - Soát vé")
FR([
    ["FR-801", "Nhân viên quét QR bằng camera điện thoại trên /staff/check-in.", "P1"],
    ["FR-802", "Kết quả: hợp lệ (phim, phòng, ghế) hoặc lý do không hợp lệ (đã dùng, sai rạp, sai thời gian, đã hủy).", "P1"],
])
H2("4.9. M09 - Quản trị")
FR([
    ["FR-901", "Dashboard: doanh thu, vé bán, tỷ lệ lấp đầy, top phim, top rạp theo ngày/tuần/tháng.", "P1"],
    ["FR-902", "Quản lý phim: import từ TMDB theo ID, sửa thông tin, trạng thái chiếu, ngày khởi chiếu tại VN, nhãn độ tuổi.", "P0"],
    ["FR-903", "Quản lý hệ thống rạp, cụm rạp, phòng chiếu; sinh sơ đồ ghế từ template (số hàng, số cột, lối đi, hàng VIP, hàng ghế đôi).", "P0"],
    ["FR-904", "Tạo suất chiếu đơn lẻ hoặc hàng loạt; kiểm tra trùng giờ (BR-50). Đánh dấu suất <b>cao tải</b> để bật phòng chờ ảo.", "P0"],
    ["FR-905", "Hủy suất chiếu: hoàn tiền hàng loạt và gửi email (BR-33).", "P1"],
    ["FR-906", "Bảng giá: giá cơ bản theo loại ghế và định dạng, hệ số ngày, khung giờ, ngày lễ.", "P0"],
    ["FR-907", "Quản lý combo và voucher (mã, % hoặc số tiền, mức giảm tối đa, đơn tối thiểu, thời hạn, tổng lượt, lượt mỗi email/SĐT).", "P1"],
    ["FR-908", "Quản lý booking: tìm theo mã, email, SĐT; lịch sử trạng thái; hoàn tiền thủ công.", "P0"],
    ["FR-909", "Quản lý tài khoản nội bộ (staff, manager) và phân quyền.", "P1"],
    ["FR-910", "Nhật ký thao tác quản trị (audit log).", "P2"],
])
H2("4.10. M10 - Tài khoản khách hàng (làm cuối cùng, tùy chọn)")
P("Module này được thiết kế để <b>thêm vào sau mà không phải sửa luồng đặt vé</b>: booking luôn lưu thông tin liên hệ, cột user_id cho phép rỗng.")
FR([
    ["FR-1001", "Đăng ký / đăng nhập bằng email + mật khẩu hoặc Google; xác thực email.", "P2"],
    ["FR-1002", "Khi tài khoản xác thực email thành công, tự động gắn các booking cũ có cùng email vào tài khoản.", "P2"],
    ["FR-1003", "Thông tin liên hệ tự điền sẵn khi đặt vé nếu đã đăng nhập.", "P2"],
    ["FR-1004", "Trang Vé của tôi: vé sắp tới, vé đã qua, hủy vé không cần OTP.", "P2"],
    ["FR-1005", "Giữ ghế theo user_id thay vì phiên khách khi đã đăng nhập (BR-04 áp dụng cho cả hai).", "P2"],
])

# ---------------------------------------------------------------- 5
H1("5. Quy tắc nghiệp vụ")
P("Đây là phần cốt lõi. Mỗi quy tắc phải có ít nhất một test tự động.")
H2("5.1. Giữ ghế")
TABLE(["Mã", "Quy tắc"], [
    ["BR-01", "<b>Bất biến quan trọng nhất:</b> mỗi ghế của một suất chiếu thuộc tối đa 1 booking ở trạng thái HELD, PAYMENT_PENDING, CONFIRMED hoặc CHECKED_IN. Đảm bảo ở tầng cơ sở dữ liệu, không chỉ ở code."],
    ["BR-02", "Mỗi booking có từ 1 đến 8 ghế."],
    ["BR-03", "Giữ nhiều ghế là thao tác nguyên tử: 1 ghế không còn trống thì toàn bộ yêu cầu thất bại."],
    ["BR-04", "Mỗi <b>phiên khách</b> (hoặc tài khoản, khi có) chỉ có tối đa 1 booking HELD cho mỗi suất chiếu. Giữ ghế mới thì booking HELD cũ bị hủy và nhả ghế trong cùng transaction."],
    ["BR-05", "Thời gian giữ ghế 10 phút (cấu hình được). Hết hạn thì booking EXPIRED, ghế về AVAILABLE."],
    ["BR-06", "Ghế đôi bán theo cặp."],
    ["BR-07", "Không để lại một ghế trống đơn lẻ giữa hai ghế đã chọn/đã bán trong cùng hàng, hoặc giữa ghế đã chọn và lối đi (trừ khi hàng đã gần đầy). Kiểm tra ở cả frontend và backend."],
    ["BR-08", "Ngừng bán sau giờ bắt đầu 15 phút; suất CANCELLED không bán."],
    ["BR-09", "Rate limit giữ ghế: 10 yêu cầu/phút mỗi phiên khách và 30 yêu cầu/phút mỗi IP. Mỗi IP tối đa 3 booking HELD cùng lúc trên toàn hệ thống."],
    ["BR-10", "Mọi yêu cầu giữ ghế và tạo thanh toán kèm Idempotency-Key; gửi lại cùng key thì trả về kết quả cũ."],
], [11, 89])
H2("5.2. Thông tin khách hàng")
TABLE(["Mã", "Quy tắc"], [
    ["BR-15", "Họ tên, email, SĐT là bắt buộc trước khi tạo thanh toán; lưu trên booking và không sửa được sau khi thanh toán (trừ admin)."],
    ["BR-16", "Email lưu dạng chữ thường; SĐT chuẩn hóa E.164 (+84...). Dùng giá trị chuẩn hóa để tra cứu và giới hạn voucher."],
    ["BR-17", "Dữ liệu cá nhân chỉ dùng cho giao dịch; không ghi vào log; che một phần khi hiển thị (n***@gmail.com, ******789)."],
], [11, 89])
H2("5.3. Giá, combo và voucher")
TABLE(["Mã", "Quy tắc"], [
    ["BR-11", "Giá ghế = giá cơ bản theo loại ghế và định dạng × hệ số ngày (thường / cuối tuần / lễ) × hệ số khung giờ; làm tròn lên bội số 1.000đ."],
    ["BR-12", "Giá chốt tại thời điểm giữ ghế và lưu vào booking; đổi bảng giá sau đó không ảnh hưởng booking đã tạo."],
    ["BR-13", "Tiền dùng số nguyên VND (BIGINT); không dùng số thực dấu phẩy động."],
    ["BR-14", "Voucher hợp lệ khi: còn hạn, còn lượt tổng, email/SĐT còn lượt, đơn đạt giá trị tối thiểu, đúng phạm vi phim/rạp. Mỗi booking tối đa 1 voucher; tổng tiền không âm."],
    ["BR-18", "Lượt voucher được tạm giữ khi áp vào booking, chỉ tính là đã dùng khi thanh toán thành công; trả lại khi booking hết hạn, hủy, hoàn tiền. Không được vượt tổng lượt kể cả khi nhiều người áp cùng lúc."],
], [11, 89])
H2("5.4. Thanh toán")
TABLE(["Mã", "Quy tắc"], [
    ["BR-20", "Bắt đầu thanh toán: booking chuyển PAYMENT_PENDING, thời hạn giữ được gia hạn thêm 5 phút (một lần)."],
    ["BR-21", "IPN phải kiểm tra chữ ký; sai chữ ký thì từ chối và ghi log cảnh báo bảo mật."],
    ["BR-22", "IPN idempotent theo mã giao dịch của cổng: nhận lại IPN đã xử lý thì trả về kết quả cũ."],
    ["BR-23", "Số tiền IPN phải bằng tổng tiền booking; lệch thì không xác nhận, đánh dấu đối soát."],
    ["BR-24", "Tiền về khi booking đã EXPIRED: nếu mọi ghế vẫn trống thì giữ lại và xác nhận; nếu không thì REFUND_PENDING, hoàn tiền tự động, gửi email giải thích."],
    ["BR-25", "Return URL chỉ để hiển thị; trạng thái booking chỉ đổi dựa trên IPN hoặc API truy vấn giao dịch."],
], [11, 89])
H2("5.5. Hủy vé và hoàn tiền")
TABLE(["Mã", "Quy tắc"], [
    ["BR-30", "Hủy trước giờ chiếu từ 24 giờ: hoàn 100%; từ 2 đến dưới 24 giờ: hoàn 50%; dưới 2 giờ hoặc đã check-in: không được hủy. Combo hoàn cùng tỷ lệ."],
    ["BR-31", "Khách hủy phải xác thực OTP gửi tới email của booking (hoặc đã đăng nhập đúng tài khoản sở hữu, khi có M10)."],
    ["BR-32", "Hủy toàn bộ booking, không hủy từng ghế. Ghế về AVAILABLE ngay; lượt voucher được trả lại."],
    ["BR-33", "Suất chiếu bị hủy bởi rạp: hoàn 100% mọi booking CONFIRMED."],
], [11, 89])
H2("5.6. Soát vé")
TABLE(["Mã", "Quy tắc"], [
    ["BR-40", "QR chứa mã vé được ký (HMAC); không chứa thông tin cá nhân."],
    ["BR-41", "Mỗi vé chỉ check-in thành công một lần, kể cả khi hai thiết bị quét đồng thời."],
    ["BR-42", "Chỉ check-in từ 30 phút trước đến 30 phút sau giờ bắt đầu, đúng cụm rạp của nhân viên."],
], [11, 89])
H2("5.7. Suất chiếu và quản trị")
TABLE(["Mã", "Quy tắc"], [
    ["BR-50", "Hai suất chiếu cùng phòng không chồng giờ. Thời gian chiếm phòng = thời lượng phim + 15 phút quảng cáo + 15 phút dọn phòng. Đảm bảo bằng exclusion constraint."],
    ["BR-51", "Định dạng suất chiếu phải được phòng hỗ trợ."],
    ["BR-52", "Không xóa suất chiếu đã có booking; chỉ hủy (kích hoạt BR-33)."],
    ["BR-53", "Phim tự chuyển trạng thái: sắp chiếu → đang chiếu vào ngày khởi chiếu; đang chiếu → ngừng chiếu khi không còn suất trong 7 ngày tới."],
], [11, 89])
H2("5.8. Phòng chờ ảo và AI")
TABLE(["Mã", "Quy tắc"], [
    ["BR-55", "Suất chiếu được đánh dấu cao tải (thủ công hoặc tự động khi lượt vào trang đặt vé vượt ngưỡng, ví dụ 500 người/phút) thì bật phòng chờ."],
    ["BR-56", "Phòng chờ cấp lượt vào theo thứ tự đến (FIFO), mỗi đợt N người (cấu hình), token vào có hiệu lực 15 phút; API giữ ghế từ chối yêu cầu không có token hợp lệ khi phòng chờ đang bật."],
    ["BR-60", "Trợ lý AI chỉ dùng công cụ đọc; mọi thao tác thay đổi dữ liệu do khách tự thực hiện trên giao diện."],
    ["BR-61", "AI chỉ trả lời số liệu (giá, giờ chiếu, ghế trống) lấy từ công cụ; không tự suy đoán."],
    ["BR-62", "Giới hạn trợ lý AI: 20 tin nhắn/giờ mỗi phiên khách."],
], [11, 89])

# ---------------------------------------------------------------- 6
H1("6. Máy trạng thái")
H2("6.1. Trạng thái ghế theo suất chiếu")
CODE("""
             giữ ghế (BR-01..10)              thanh toán thành công
 AVAILABLE ───────────────────────► HELD ─────────────────────────► SOLD
     ▲                                │                               │
     │  hết hạn / khách bỏ giữ        │                               │
     └────────────────────────────────┘                               │
     ▲                                                                │
     └──────────────────── hủy vé / suất chiếu bị hủy ────────────────┘

 BLOCKED : ghế không bán (hỏng, dành riêng) - chỉ admin thay đổi
""")
H2("6.2. Trạng thái booking")
CODE("""
 HELD ──(nhập thông tin, thanh toán)──► PAYMENT_PENDING ──(IPN OK)──► CONFIRMED
  │                                       │                             │
  ├─(hết hạn)──────► EXPIRED              ├─(IPN lỗi)► PAYMENT_FAILED   ├─(quét QR)► CHECKED_IN
  └─(khách bỏ giữ)─► CANCELLED            └─(hết hạn)► EXPIRED          ├─(hủy+OTP)► CANCELLED
                                                          │             └─(suất hủy)► CANCELLED
                                                          │                       │
             tiền về muộn, ghế đã mất (BR-24)             ▼          hoàn tiền    ▼
                                                   REFUND_PENDING ──► REFUNDED | REFUND_FAILED
""")
P("Mọi lần chuyển trạng thái được ghi vào <b>booking_status_history</b> (cũ, mới, lý do, tác nhân, thời điểm). "
  "Chuyển trạng thái không hợp lệ phải bị từ chối ở tầng domain.")
H2("6.3. Trạng thái thanh toán và suất chiếu")
CODE("""
 Payment : CREATED ──► PENDING ──► SUCCEEDED ──► REFUND_REQUESTED ──► REFUNDED | REFUND_FAILED
                              └──► FAILED | CANCELLED_BY_USER | AMOUNT_MISMATCH
 Showtime: SCHEDULED ──► ON_SALE ──► CLOSED (giờ bắt đầu + 15') ──► FINISHED ;  * ──► CANCELLED
""")

# ---------------------------------------------------------------- 7
H1("7. Dữ liệu")
H2("7.1. Các thực thể chính")
TABLE(["Nhóm", "Bảng", "Trường chính / ghi chú"], [
    ["Danh mục", "province, genre", "Tỉnh/thành, thể loại"],
    ["Phim", "movie, movie_genre", "tmdb_id (unique), slug, title_vi, original_title, runtime_min, release_date_vn, age_rating, status, poster_url, trailer_key, directors, casts (text/JSONB), tmdb_score"],
    ["Rạp", "cinema_chain, cinema, room, seat", "cinema: chain_id, province_id, address, lat, lng; room: formats; seat: row_label, col_no, type, pair_seat_id, is_sellable"],
    ["Lịch chiếu", "showtime, showtime_seat", "showtime: movie_id, room_id, start_at, occupied_until, format, language, status, high_demand; showtime_seat: showtime_id, seat_id, status, booking_id, version"],
    ["Giá", "price_rule, holiday", "seat_type, format, day_type, time_slot, base_price, multiplier"],
    ["Đặt vé", "booking, booking_seat, booking_combo, booking_status_history", "booking: code, guest_session_id, user_id (nullable), customer_name, email, phone, showtime_id, status, hold_expires_at, subtotal, discount, total, idempotency_key"],
    ["Thanh toán", "payment, payment_callback, refund", "provider, provider_txn_id (unique), amount, status, raw_payload"],
    ["Khuyến mãi", "voucher, voucher_redemption, combo", "voucher: code, total_limit, used_count, reserved_count, per_contact_limit, min_order, valid_from/to"],
    ["Vé", "ticket, cancel_otp", "ticket: booking_id, seat_id, qr_token, checked_in_at, checked_in_by; cancel_otp: booking_id, otp_hash, expires_at, attempts"],
    ["Nội bộ", "staff_account, role", "Tài khoản admin/manager/staff, cinema_id hoặc chain_id được phân công"],
    ["Hệ thống", "outbox_event, processed_event, audit_log", "outbox: aggregate_type, aggregate_id, event_type, payload (JSONB); processed_event: (consumer, event_id)"],
    ["Thống kê", "movie_sales_daily (read model)", "Được cập nhật bởi consumer Kafka, phục vụ bảng xếp hạng và dashboard"],
    ["AI", "kb_document (pgvector)", "Tài liệu chính sách và embedding cho RAG"],
    ["Tài khoản (M10)", "customer_account", "Thêm ở giai đoạn cuối; booking.user_id tham chiếu tới bảng này"],
], [14, 28, 58])
H2("7.2. Ràng buộc dữ liệu quan trọng")
CODE("""
-- BR-01: mỗi ghế của mỗi suất là đúng 1 dòng; giữ ghế = UPDATE có điều kiện
ALTER TABLE showtime_seat ADD CONSTRAINT uq_showtime_seat UNIQUE (showtime_id, seat_id);
UPDATE showtime_seat SET status='HELD', booking_id=:b, version=version+1
 WHERE showtime_id=:s AND seat_id = ANY(:seats) AND status='AVAILABLE';
-- số dòng bị ảnh hưởng phải = số ghế yêu cầu, nếu không thì ROLLBACK (BR-03)

-- BR-04: một phiên khách chỉ có 1 booking HELD mỗi suất
CREATE UNIQUE INDEX uq_one_hold ON booking (showtime_id, guest_session_id)
  WHERE status = 'HELD';

-- BR-50: không chồng giờ suất chiếu trong cùng phòng
CREATE EXTENSION IF NOT EXISTS btree_gist;
ALTER TABLE showtime ADD CONSTRAINT no_overlap
  EXCLUDE USING gist (room_id WITH =, tstzrange(start_at, occupied_until) WITH &&)
  WHERE (status <> 'CANCELLED');

-- BR-18: voucher không vượt tổng lượt
UPDATE voucher SET reserved_count = reserved_count + 1
 WHERE id=:v AND used_count + reserved_count < total_limit;

-- BR-22 và consumer idempotent
ALTER TABLE payment ADD CONSTRAINT uq_provider_txn UNIQUE (provider, provider_txn_id);
CREATE TABLE processed_event (consumer TEXT, event_id UUID, PRIMARY KEY (consumer, event_id));
""")
H2("7.3. Nguồn dữ liệu")
TABLE(["Dữ liệu", "Nguồn", "Cách nạp"], [
    ["Phim, poster, trailer, đạo diễn, diễn viên, điểm", "TMDB API: now_playing, upcoming (region=VN, language=vi-VN), movie details + credits + videos", "Job đồng bộ hằng ngày (@Scheduled), upsert theo tmdb_id; retry, timeout, rate limit bằng Resilience4j. Không crawl website."],
    ["Tỉnh/thành", "Danh mục hành chính Việt Nam", "Seed Flyway"],
    ["Hệ thống rạp, cụm rạp, phòng, ghế", "Tự sinh (thương hiệu hư cấu)", "DataSeeder: 5 hệ thống rạp, 30 cụm rạp tại 8 tỉnh/thành, 4-8 phòng mỗi cụm"],
    ["Suất chiếu", "Tự sinh", "DataSeeder lấp lịch 7 ngày tới (9h-24h) theo BR-50; job hằng đêm sinh ngày mới"],
    ["Booking lịch sử", "Datafaker", "Chỉ ở profile dev/demo để dashboard và bảng xếp hạng có số liệu"],
], [22, 36, 42])

# ---------------------------------------------------------------- 8
H1("8. Kiến trúc và công nghệ")
H2("8.1. Kiến trúc tổng thể")
P("Bắt đầu bằng <b>modular monolith</b> (một ứng dụng Spring Boot, chia module theo domain: catalog, showtime, booking, payment, notification, "
  "analytics, assistant, admin). Khi nghiệp vụ ổn định, tách <b>booking</b>, <b>payment</b>, <b>notification</b> thành service riêng giao tiếp qua Kafka.")
CODE("""
                      ┌──────────────────────────┐
 Browser ──HTTPS────► │ CDN + Next.js (SSR/ISR)  │
    │                 └────────────┬─────────────┘
    │ WebSocket (STOMP)            │ REST
    ▼                              ▼
 ┌──────────────── Ingress + rate limit + waiting room ─────────────┐
 │                                                                  │
 │  catalog     showtime     booking      payment      assistant    │
 │  (movies,    (lich chieu, (giu ghe,    (VNPay,      (Spring AI,  │
 │   cinemas)    so do ghe)   saga)        IPN)         tool call)  │
 │                                                                  │
 │  notification (email)     analytics (ranking)          admin     │
 └──────┬─────────────────────────┬─────────────────────────────────┘
        │                         │
   PostgreSQL (+pgvector)    Redis (cache, rate limit, waiting queue,
        │                           pub/sub, session)
   outbox ──Debezium CDC──► Apache Kafka ──► consumers: notification,
                                             seat-map realtime, analytics,
                                             refund, cache invalidation
""")
H2("8.2. Trọng tâm system design")
TABLE(["Bài toán", "Giải pháp trong CineBook", "Kiến thức thu được"], [
    ["Bán trùng ghế", "UPDATE có điều kiện + unique constraint; so sánh với Redis lock (Redisson) và optimistic locking; đo bằng k6", "Isolation level, locking, so sánh trade-off các cách chống race condition"],
    ["Ghi DB và gửi Kafka lệch nhau", "Transactional Outbox + Debezium CDC", "Dual-write problem, CDC, at-least-once"],
    ["Message trùng / sai thứ tự", "processed_event, key theo aggregate id, retry + DLT", "Idempotent consumer, partitioning, ordering"],
    ["Giao dịch qua nhiều bước", "Saga (orchestration trong booking) với bước bù trừ", "Distributed transaction, compensation"],
    ["Mở bán gây quá tải", "Phòng chờ ảo (Redis sorted set + token), rate limit token bucket, Turnstile", "Load shedding, backpressure, fairness"],
    ["Đọc nhiều hơn ghi", "CDN/ISR cho trang phim; Redis cache lịch chiếu; snapshot sơ đồ ghế + delta qua WebSocket", "Cache-aside, invalidation qua sự kiện, CQRS read model"],
    ["Realtime trên nhiều pod", "WebSocket broadcast qua Redis pub/sub hoặc Kafka; sticky session không bắt buộc", "Scale stateful connection"],
    ["Job hết hạn chạy nhiều pod", "SELECT ... FOR UPDATE SKIP LOCKED hoặc ShedLock; hoặc Redis keyspace / delayed message", "Distributed scheduling"],
    ["Cổng thanh toán không tin cậy", "IPN idempotent, kiểm tra chữ ký, job đối soát, hoàn tiền tự động", "Webhook reliability, reconciliation"],
    ["Bảng xếp hạng realtime", "Consumer Kafka cộng dồn vé bán vào Redis sorted set + bảng thống kê", "Stream aggregation, read model"],
    ["Dữ liệu lớn dần", "Index phù hợp; partition showtime_seat theo tháng; archive booking cũ", "Indexing, partitioning, data lifecycle"],
], [18, 46, 36])
H2("8.3. Công nghệ")
TABLE(["Tầng", "Công nghệ"], [
    ["Frontend", "Next.js (App Router, TypeScript), Tailwind CSS, shadcn/ui, TanStack Query, Zustand, @stomp/stompjs, react-hook-form + zod, Leaflet, html5-qrcode"],
    ["Backend", "Java 21, Spring Boot 3: Web, Validation, Data JPA, Security, WebSocket (STOMP), Spring Kafka, Spring AI, Actuator; MapStruct; Resilience4j; Bucket4j; springdoc-openapi"],
    ["Dữ liệu", "PostgreSQL 16 (pgvector, btree_gist, unaccent, pg_trgm), Flyway, Redis 7 + Redisson"],
    ["Messaging", "Apache Kafka (KRaft), Debezium (Outbox CDC), JSON event có version"],
    ["Kiểm thử", "JUnit 5, AssertJ, Testcontainers, ArchUnit, Playwright, k6, Postman CLI"],
    ["DevOps", "Docker, Docker Compose, GitHub Actions, GHCR, Kubernetes (kind / k3s), Helm, ArgoCD, Strimzi, CloudNativePG, cert-manager, Ingress NGINX"],
    ["Quan sát", "Prometheus, Grafana, Loki, OpenTelemetry + Tempo, Alertmanager (Telegram)"],
], [16, 84])
H2("8.4. Sự kiện Kafka")
TABLE(["Topic", "Phát khi", "Consumer"], [
    ["booking.held", "Giữ ghế thành công", "seat-map realtime"],
    ["booking.released", "Booking hết hạn hoặc khách bỏ giữ", "seat-map realtime, voucher (trả lượt)"],
    ["payment.succeeded / payment.failed", "Xử lý IPN xong", "booking (xác nhận / đánh dấu thất bại)"],
    ["booking.confirmed", "Booking CONFIRMED", "ticket (sinh QR), notification (email vé), analytics"],
    ["booking.cancelled", "Hủy vé đã thanh toán", "refund, seat-map realtime, voucher, notification, analytics"],
    ["refund.completed / refund.failed", "Kết quả hoàn tiền", "booking, notification"],
    ["showtime.cancelled", "Admin hủy suất chiếu", "booking (hủy hàng loạt), refund, notification"],
    ["showtime.changed, movie.updated", "Thay đổi lịch chiếu / phim", "cache invalidation, ISR revalidate"],
    ["otp.requested", "Khách yêu cầu hủy vé", "notification (email OTP)"],
], [26, 34, 40])
P("Quy ước: key là id của aggregate (booking_id, showtime_id) để đảm bảo thứ tự; mọi message có event_id (UUID), event_type, occurred_at, version. "
  "Consumer lỗi được retry có backoff rồi chuyển sang topic <b>.DLT</b>.")
H2("8.5. Luồng đặt vé (Saga)")
CODE("""
 0. (nếu suất cao tải) GET /queue/{showtimeId} → chờ đến lượt → nhận access token
 1. POST /bookings/hold {showtimeId, seatIds}  + Idempotency-Key + cookie phiên khách
      TX: kiểm tra BR-02..09 → UPDATE showtime_seat AVAILABLE→HELD
          → INSERT booking(HELD, hold_expires_at) → INSERT outbox(booking.held)
 2. PATCH /bookings/{id} {combos, voucherCode, customerName, email, phone}
      → tính lại giá, tạm giữ lượt voucher
 3. POST /bookings/{id}/payments → PAYMENT_PENDING (+5 phút) → URL VNPay
 4. VNPay ──IPN──► /payments/vnpay/ipn
      TX: chữ ký + idempotent + số tiền → payment SUCCEEDED → outbox(payment.succeeded)
 5. booking consumer: TX: CONFIRMED, ghế HELD→SOLD, voucher reserved→used
                         → outbox(booking.confirmed)
 6. ticket + notification consumer: sinh QR, gửi email vé
 Bù trừ: hết hạn → EXPIRED, nhả ghế, trả lượt voucher; tiền về muộn → BR-24
""")
H2("8.6. API chính (REST, tiền tố /api/v1)")
TABLE(["Nhóm", "Endpoint"], [
    ["Phim", "GET /movies?status=&genre=&sort=, /movies/{slug}, /movies/{id}/showtimes?province=&date=&format="],
    ["Rạp", "GET /provinces, /chains, /chains/{slug}/cinemas, /cinemas/{slug}, /cinemas/{id}/showtimes?date="],
    ["Lịch chiếu", "GET /showtimes?province=&date=&chain=&cinema=&movie=&format=, /showtimes/{id}/seats"],
    ["Phòng chờ", "POST /queue/{showtimeId}/join, GET /queue/{showtimeId}/status"],
    ["Đặt vé", "POST /bookings/hold; GET/PATCH /bookings/{id}; DELETE /bookings/{id}/hold; POST /bookings/{id}/payments"],
    ["Thanh toán", "GET /payments/vnpay/return; GET /payments/vnpay/ipn"],
    ["Tra cứu / hủy", "POST /lookup {code, emailOrPhone}; GET /tickets/{token}; POST /bookings/{code}/resend-email; POST /bookings/{code}/cancel-otp; POST /bookings/{code}/cancel {otp}"],
    ["Khác", "GET /rankings?period=; GET /search?q=; POST /assistant/chat (SSE)"],
    ["Soát vé", "POST /staff/check-in {qrToken}"],
    ["Admin", "/admin/movies, /admin/cinemas, /admin/rooms, /admin/showtimes (+ /bulk), /admin/prices, /admin/vouchers, /admin/combos, /admin/bookings, /admin/staff, /admin/stats"],
    ["Realtime", "WebSocket /ws, subscribe /topic/showtimes/{id}/seats"],
], [14, 86])
P("Lỗi trả về theo RFC 7807 (Problem Details) với mã nghiệp vụ, ví dụ SEAT_UNAVAILABLE, HOLD_EXPIRED, VOUCHER_EXHAUSTED, CANCEL_NOT_ALLOWED, QUEUE_TOKEN_REQUIRED.")

# ---------------------------------------------------------------- 9
H1("9. Yêu cầu phi chức năng")
TABLE(["Mã", "Nhóm", "Yêu cầu"], [
    ["NFR-01", "Tính đúng", "Không bán trùng ghế, không vượt lượt voucher, không xác nhận một thanh toán hai lần, trong mọi kịch bản đồng thời và lỗi giữa chừng."],
    ["NFR-02", "Hiệu năng", "API giữ ghế p95 &lt; 300ms với 1.000 người dùng đồng thời; API lịch chiếu p95 &lt; 100ms (cache); Lighthouse performance ≥ 85 trên mobile."],
    ["NFR-03", "Realtime", "Thay đổi trạng thái ghế hiển thị cho người khác trong &lt; 1 giây."],
    ["NFR-04", "Mở rộng", "Service không lưu trạng thái trong bộ nhớ; scale ngang bằng HPA; WebSocket hoạt động khi chạy nhiều pod."],
    ["NFR-05", "Độ tin cậy", "Không mất sự kiện khi service khởi động lại; job nền không xử lý trùng khi chạy nhiều pod."],
    ["NFR-06", "Bảo mật", "OWASP Top 10; cookie phiên khách ký số, httpOnly, SameSite; token vé và OTP không đoán được; OTP lưu dạng băm; rate limit; secret trong Kubernetes Secret."],
    ["NFR-07", "Dữ liệu cá nhân", "Tên, email, SĐT chỉ dùng cho giao dịch; không ghi log; che khi hiển thị; tự xóa/ẩn danh sau 12 tháng."],
    ["NFR-08", "Bảo mật AI", "Chống prompt injection: công cụ chỉ đọc, không đưa dữ liệu cá nhân vào ngữ cảnh."],
    ["NFR-09", "SEO", "SSR/ISR trang phim, rạp; meta, Open Graph, sitemap.xml, schema.org (Movie, Event)."],
    ["NFR-10", "Khả dụng", "Responsive (mobile trước); WCAG 2.1 AA ở luồng đặt vé."],
    ["NFR-11", "Quan sát", "Log JSON có traceId; metric nghiệp vụ (ghế giữ, booking thành công/thất bại, IPN lỗi, consumer lag, độ dài hàng chờ); tracing xuyên service; cảnh báo khi lỗi &gt; 2% hoặc lag &gt; 1.000."],
    ["NFR-12", "Sao lưu", "Backup PostgreSQL hằng ngày + WAL; diễn tập khôi phục (RPO ≤ 15 phút, RTO ≤ 1 giờ)."],
    ["NFR-13", "Chất lượng mã", "Độ phủ test booking/payment ≥ 80%; ArchUnit kiểm tra ranh giới module; static analysis trong CI."],
    ["NFR-14", "Thời gian", "Lưu UTC (timestamptz), hiển thị Asia/Ho_Chi_Minh."],
], [11, 15, 74])

# ---------------------------------------------------------------- 10
H1("10. Kiểm thử và tiêu chí nghiệm thu")
H2("10.1. Chiến lược kiểm thử")
TABLE(["Loại", "Công cụ", "Phạm vi"], [
    ["Unit", "JUnit 5, AssertJ", "Tính giá, kiểm tra ghế lẻ (BR-07), chính sách hoàn tiền, máy trạng thái"],
    ["Integration", "Testcontainers (Postgres, Redis, Kafka)", "Ràng buộc DB, Outbox, consumer idempotent, IPN"],
    ["Concurrency", "ExecutorService + CountDownLatch", "100 luồng giữ 1 ghế: đúng 1 thành công; 100 luồng dùng voucher còn 10 lượt: đúng 10"],
    ["API", "MockMvc, Postman CLI", "Mã lỗi, validate, rate limit, phân quyền admin"],
    ["E2E", "Playwright", "Chọn suất → giữ ghế → nhập thông tin → thanh toán sandbox → nhận vé → tra cứu → hủy bằng OTP"],
    ["Tải", "k6", "Mục 10.2"],
    ["Chaos", "Kill pod, tắt Kafka broker", "Tự phục hồi, không mất sự kiện, không sai dữ liệu"],
], [14, 30, 56])
H2("10.2. Kịch bản tải (k6)")
TABLE(["Kịch bản", "Mô tả", "Tiêu chí đạt"], [
    ["Tranh 1 ghế", "1.000 người dùng ảo cùng giữ ghế A5 của một suất", "Đúng 1 phản hồi thành công; DB có đúng 1 booking giữ A5"],
    ["Mở bán", "5.000 người dùng ảo tranh 200 ghế trong 60 giây", "Không ghế nào có 2 booking; tổng ghế bán ≤ 200; p95 &lt; 300ms"],
    ["Phòng chờ", "20.000 người vào suất cao tải", "Hệ thống không quá tải; thứ tự vào đúng FIFO; tỷ lệ lỗi &lt; 1%"],
    ["Gửi trùng", "Mỗi người gửi 3 lần cùng Idempotency-Key", "Mỗi người chỉ có 1 booking"],
    ["Hết hạn", "Giữ 500 ghế rồi bỏ đi", "Sau 11 phút toàn bộ ghế về AVAILABLE"],
    ["IPN trùng", "Gửi lại IPN thành công 5 lần đồng thời", "CONFIRMED một lần, 1 email, 1 bộ vé"],
    ["Tăng tải", "0 → 3.000 người dùng trong 5 phút trên Kubernetes", "HPA tăng pod; tỷ lệ lỗi &lt; 1%"],
], [16, 42, 42])
CODE("""
-- Kiểm tra bất biến sau mỗi lần chạy: phải trả về 0 dòng
SELECT b.showtime_id, bs.seat_id, COUNT(*)
  FROM booking_seat bs JOIN booking b ON b.id = bs.booking_id
 WHERE b.status IN ('HELD','PAYMENT_PENDING','CONFIRMED','CHECKED_IN')
 GROUP BY b.showtime_id, bs.seat_id HAVING COUNT(*) > 1;
""")
H2("10.3. Tiêu chí nghiệm thu")
UL(["Mọi yêu cầu P0 và P1 hoạt động trên Kubernetes, truy cập qua tên miền HTTPS.",
    "Mọi quy tắc BR-xx có ít nhất một test tự động.",
    "Các kịch bản tải đạt tiêu chí; có dashboard Grafana minh họa.",
    "CI/CD: push lên main thì tự động test, build image, triển khai qua ArgoCD.",
    "Tài liệu: README, sơ đồ kiến trúc, OpenAPI, ADR (ghi lại các quyết định thiết kế và trade-off), chạy local bằng một lệnh."])

# ---------------------------------------------------------------- 11
H1("11. Triển khai và vận hành (DevOps)")
H2("11.1. Môi trường")
TABLE(["Môi trường", "Hạ tầng", "Mục đích"], [
    ["local", "Docker Compose: Postgres, Redis, Kafka, Debezium, Mailpit", "Phát triển hằng ngày"],
    ["dev-k8s", "kind / minikube trên máy cá nhân", "Học Helm chart, Strimzi, ArgoCD"],
    ["staging/prod", "k3s trên 1-3 VPS; Cloudflare DNS; cert-manager", "Demo công khai, load test"],
], [16, 50, 34])
H2("11.2. CI/CD")
UL(["GitHub Actions: lint → unit test → integration test (Testcontainers) → build image (multi-stage hoặc Jib) → quét lỗ hổng (Trivy) → đẩy GHCR.",
    "Cập nhật tag image trong repo GitOps; ArgoCD tự đồng bộ cluster.",
    "Flyway migration chạy trước khi rollout; rolling update với readiness probe; rollback bằng ArgoCD."])
H2("11.3. Quản trị server")
UL(["SSH chỉ dùng key, tắt đăng nhập root và mật khẩu; UFW chỉ mở 22/80/443; fail2ban.",
    "Cập nhật bảo mật tự động; giám sát node bằng node-exporter.",
    "Hạ tầng dưới dạng code: Terraform (VPS, DNS), Ansible (cài k3s, hardening).",
    "Backup Postgres (CloudNativePG/WAL-G) lên S3/R2; diễn tập khôi phục định kỳ."])

# ---------------------------------------------------------------- 12
H1("12. Lộ trình phát triển")
TABLE(["Giai đoạn", "Nội dung", "Kết quả"], [
    ["0. Nền tảng (1 tuần)", "Monorepo, Docker Compose, Spring Boot + Next.js skeleton, Flyway, CI cơ bản, admin login đơn giản", "Chạy local bằng một lệnh"],
    ["1. Danh mục (2 tuần)", "M01 phim + đồng bộ TMDB, M02 rạp và lịch chiếu, DataSeeder, admin CRUD", "Website xem phim, rạp, lịch chiếu có dữ liệu"],
    ["2. Giữ ghế (2 tuần)", "Phiên khách, sơ đồ ghế, BR-01..10, hết hạn, test concurrency, k6 tranh 1 ghế", "Chứng minh không bán trùng ghế"],
    ["3. Thanh toán (2 tuần)", "Thông tin khách, VNPay sandbox, IPN idempotent, vé QR, email, tra cứu vé", "Luồng đặt vé hoàn chỉnh không cần tài khoản"],
    ["4. Kafka (2 tuần)", "Outbox + Debezium, consumer idempotent, DLT, sơ đồ ghế realtime, bảng xếp hạng", "Kiến trúc hướng sự kiện"],
    ["5. Nghiệp vụ mở rộng (2 tuần)", "Combo, voucher, hủy vé OTP + hoàn tiền, đối soát, soát vé, dashboard", "Đủ nghiệp vụ rạp"],
    ["6. Chịu tải (2 tuần)", "Cache nhiều tầng, rate limit, phòng chờ ảo, Turnstile; k6 kịch bản mở bán", "Hệ thống chịu được mở bán"],
    ["7. AI (1-2 tuần)", "Spring AI: tìm suất, gợi ý ghế, RAG chính sách", "Trợ lý AI an toàn"],
    ["8. Kubernetes (3 tuần)", "Tách booking/payment/notification; Helm, Strimzi, CloudNativePG, ArgoCD, HPA", "Chạy trên k3s với tên miền thật"],
    ["9. Quan sát (1-2 tuần)", "Prometheus, Grafana, Loki, Tempo, cảnh báo; load test và chaos test trên cluster", "Báo cáo hiệu năng"],
    ["10. Tài khoản khách (tùy chọn)", "M10: đăng ký/đăng nhập, gắn booking cũ theo email, Vé của tôi", "Không ảnh hưởng luồng đặt vé đã có"],
], [22, 50, 28])
NOTE("Tổng thời gian ước tính: khoảng 4-5 tháng làm bán thời gian (giảm khoảng 1 tháng so với v1.0 nhờ lược bỏ cộng đồng, tin tức, trang nghệ sĩ).")

# ---------------------------------------------------------------- 13
H1("13. Rủi ro và lưu ý")
TABLE(["Rủi ro", "Ảnh hưởng", "Giảm thiểu"], [
    ["Phạm vi quá lớn cho một người", "Dự án bỏ dở", "Làm theo giai đoạn; hoàn thiện P0 trước; giữ modular monolith lâu nhất có thể"],
    ["Không có tài khoản nên dễ bị bot gom vé", "Khách thật không mua được", "Rate limit theo phiên và IP, giới hạn số booking HELD mỗi IP, Turnstile, phòng chờ ảo"],
    ["Khách nhập sai email", "Không nhận được vé", "Hiển thị vé ngay trên trang kết quả; tra cứu bằng mã + SĐT; gửi lại email"],
    ["Vi phạm điều khoản TMDB hoặc thương hiệu rạp", "Pháp lý", "Ghi nhận nguồn TMDB; chỉ dùng thương hiệu hư cấu; không crawl website khác"],
    ["Bị hiểu nhầm là website bán vé thật", "Người dùng thật bị ảnh hưởng", "Banner demo trên mọi trang; chỉ thanh toán sandbox"],
    ["Chi phí LLM", "Phát sinh chi phí", "Rate limit (BR-62), cache câu trả lời phổ biến, chọn model phù hợp"],
    ["Kafka/Kubernetes phức tạp", "Chậm tiến độ", "Chỉ đưa vào sau khi nghiệp vụ chạy đúng trên monolith"],
], [26, 18, 56])

doc = Doc(OUT)
doc.multiBuild(story)
print("OK", OUT)
