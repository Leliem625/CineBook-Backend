package com.cinebook.payment.service;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.cinebook.payment.entity.CallbackType;
import com.cinebook.payment.entity.PaymentCallback;
import com.cinebook.payment.entity.PaymentProvider;
import com.cinebook.payment.repository.PaymentCallbackRepository;

import lombok.RequiredArgsConstructor;

/**
 * Ghi lại mọi callback của cổng (FR-503, BR-21) trong transaction RIÊNG: xử lý callback có lỗi và rollback
 * thì bản ghi callback vẫn còn để đối soát.
 * <p>
 * Phải gọi TRƯỚC khi khóa dòng payment ở transaction ngoài: khóa ngoại payment_id cần đọc dòng payment đó,
 * nếu transaction ngoài đã khóa ghi thì hai bên chờ nhau (deadlock).
 */
@Component
@RequiredArgsConstructor
public class PaymentCallbackRecorder {

    private final PaymentCallbackRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(CallbackType type, String txnRef, Map<String, String> payload, boolean signatureValid,
                       Long paymentId) {
        PaymentCallback callback = new PaymentCallback();
        callback.setProvider(PaymentProvider.VNPAY);
        callback.setCallbackType(type);
        callback.setTxnRef(txnRef);
        callback.setPayload(payload);
        callback.setSignatureValid(signatureValid);
        callback.setPaymentId(paymentId);
        repository.save(callback);
    }
}
