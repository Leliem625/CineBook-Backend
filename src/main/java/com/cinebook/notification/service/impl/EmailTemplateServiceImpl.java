package com.cinebook.notification.service.impl;

import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.cinebook.booking.dto.BookingResponse;
import com.cinebook.notification.dto.EmailContent;
import com.cinebook.notification.service.EmailTemplateService;
import com.cinebook.payment.event.PaymentSucceededEvent;
import com.cinebook.showtime.dto.SeatMapResponse;

/**
 * Cài đặt {@link EmailTemplateService} bằng chuỗi Java (chưa dùng engine template). Mọi dữ liệu do khách hoặc
 * nguồn ngoài nhập (tên, tên phim, địa chỉ rạp...) đều được escape HTML trước khi đưa vào.
 */
@Service
public class EmailTemplateServiceImpl implements EmailTemplateService {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy");
    private static final DateTimeFormatter PAID_AT = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");

    @Override
    public EmailContent ticketConfirmation(BookingResponse booking, SeatMapResponse showtime,
                                           PaymentSucceededEvent payment) {
        String movie = showtime.movie() != null ? showtime.movie().title() : "Suất chiếu";
        String cinema = showtime.cinema().name();
        String address = showtime.cinema().address() == null ? "" : showtime.cinema().address();
        String when = DATE_TIME.format(showtime.startAt().atZoneSameInstant(VN));
        String format = formatLabel(showtime) ;
        String room = showtime.roomName();
        String total = money(booking.total());
        String paidAt = PAID_AT.format(payment.paidAt().atZone(VN));
        String method = payment.provider().name().equals("VNPAY") ? "VNPay" : payment.provider().name();

        String subject = "[CineBook] Vé xem phim " + movie + " - mã đặt vé " + booking.code();

        // ---- bản chữ thường
        String seatLines = booking.seats().stream()
                .map(s -> "  - Ghế " + s.row() + String.format("%02d", s.number()) + "  |  Mã vé: "
                        + (s.ticketCode() == null ? "-" : s.ticketCode().substring(0, Math.min(12, s.ticketCode().length())).toUpperCase()))
                .collect(Collectors.joining("\n"));
        String text = """
                Xin chào %s,

                Thanh toán của bạn đã thành công. Dưới đây là vé của bạn.

                MÃ ĐẶT VÉ: %s

                Phim:    %s (%s)
                Rạp:     %s
                Địa chỉ: %s
                Phòng:   %s
                Suất:    %s

                Ghế:
                %s

                Thanh toán: %s qua %s
                Mã giao dịch: %s
                Thời gian: %s

                Vui lòng xuất trình mã đặt vé khi đến rạp.
                CineBook
                """.formatted(booking.customerName(), booking.code(), movie, format, cinema, address, room, when,
                seatLines, total, method, payment.providerTxnId() == null ? "-" : payment.providerTxnId(), paidAt);

        // ---- bản HTML
        String seatRows = booking.seats().stream()
                .map(s -> "<tr><td style=\"padding:8px 12px;border-top:1px solid #e5e7eb\"><b>Gh\u1ebf "
                        + esc(s.row() + String.format("%02d", s.number())) + "</b></td>"
                        + "<td style=\"padding:8px 12px;border-top:1px solid #e5e7eb;text-align:right;font-family:monospace;color:#4b5563\">"
                        + (s.ticketCode() == null ? "-" : esc(s.ticketCode().substring(0, Math.min(12, s.ticketCode().length())).toUpperCase()))
                        + "</td></tr>")
                .collect(Collectors.joining());
        String html = """
                <div style="background:#f3f4f6;padding:24px 12px;font-family:Arial,Helvetica,sans-serif;color:#111827">
                  <div style="max-width:560px;margin:0 auto;background:#ffffff;border-radius:12px;overflow:hidden">
                    <div style="background:#10131f;color:#ffffff;padding:20px 24px">
                      <div style="font-size:20px;font-weight:bold">CineBook</div>
                      <div style="opacity:.8;margin-top:4px">Thanh to\u00e1n th\u00e0nh c\u00f4ng</div>
                    </div>
                    <div style="padding:24px">
                      <p style="margin:0 0 16px">Xin ch\u00e0o <b>%s</b>, v\u00e9 c\u1ee7a b\u1ea1n \u0111\u00e3 s\u1eb5n s\u00e0ng.</p>
                      <div style="text-align:center;background:#fef2f2;border-radius:8px;padding:16px;margin-bottom:20px">
                        <div style="color:#6b7280;font-size:13px">M\u00e3 \u0111\u1eb7t v\u00e9</div>
                        <div style="font-size:30px;font-weight:bold;letter-spacing:4px;font-family:monospace">%s</div>
                      </div>
                      <div style="font-size:18px;font-weight:bold">%s</div>
                      <div style="color:#6b7280;margin-bottom:12px">%s</div>
                      <table style="width:100%%;font-size:14px;border-collapse:collapse">
                        <tr><td style="color:#6b7280;padding:3px 0">R\u1ea1p</td><td style="text-align:right">%s</td></tr>
                        <tr><td style="color:#6b7280;padding:3px 0">\u0110\u1ecba ch\u1ec9</td><td style="text-align:right">%s</td></tr>
                        <tr><td style="color:#6b7280;padding:3px 0">Ph\u00f2ng</td><td style="text-align:right">%s</td></tr>
                        <tr><td style="color:#6b7280;padding:3px 0">Su\u1ea5t chi\u1ebfu</td><td style="text-align:right"><b>%s</b></td></tr>
                      </table>
                      <table style="width:100%%;margin-top:16px;font-size:14px;border:1px solid #e5e7eb;border-radius:8px;border-collapse:separate;border-spacing:0">
                        <tr><td style="padding:8px 12px;color:#6b7280">Gh\u1ebf</td><td style="padding:8px 12px;color:#6b7280;text-align:right">M\u00e3 v\u00e9</td></tr>
                        %s
                      </table>
                      <table style="width:100%%;margin-top:16px;font-size:14px;border-collapse:collapse">
                        <tr><td style="color:#6b7280;padding:3px 0">T\u1ed5ng thanh to\u00e1n</td><td style="text-align:right;font-size:18px"><b>%s</b></td></tr>
                        <tr><td style="color:#6b7280;padding:3px 0">Ph\u01b0\u01a1ng th\u1ee9c</td><td style="text-align:right">%s</td></tr>
                        <tr><td style="color:#6b7280;padding:3px 0">M\u00e3 giao d\u1ecbch</td><td style="text-align:right;font-family:monospace">%s</td></tr>
                        <tr><td style="color:#6b7280;padding:3px 0">Th\u1eddi gian</td><td style="text-align:right">%s</td></tr>
                      </table>
                      <p style="margin:20px 0 0;color:#6b7280;font-size:13px">Vui l\u00f2ng xu\u1ea5t tr\u00ecnh m\u00e3 \u0111\u1eb7t v\u00e9 khi \u0111\u1ebfn r\u1ea1p.</p>
                    </div>
                  </div>
                </div>
                """.formatted(esc(booking.customerName()), esc(booking.code()), esc(movie), esc(format), esc(cinema),
                esc(address), esc(room), esc(when), seatRows, esc(total), esc(method),
                esc(payment.providerTxnId() == null ? "-" : payment.providerTxnId()), esc(paidAt));

        return new EmailContent(subject, html, text);
    }

    private static String formatLabel(SeatMapResponse showtime) {
        String screen = switch (showtime.format()) {
            case F2D -> "2D";
            case F3D -> "3D";
            case IMAX -> "IMAX";
            case F4DX -> "4DX";
        };
        String language = switch (showtime.language()) {
            case SUBTITLED -> "Phụ đề";
            case DUBBED -> "Lồng tiếng";
            case ORIGINAL -> "Bản gốc";
        };
        return screen + " " + language;
    }

    private static String money(long vnd) {
        return NumberFormat.getInstance(Locale.of("vi", "VN")).format(vnd) + "đ";
    }

    /** Escape HTML cho dữ liệu đưa vào thư. */
    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
