package entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Created by chao on 2017/11/11.
 * client 向 Validator 发送的消息
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientMessage extends Message {
    private Transaction transaction;

    public ClientMessage() {
    }

    public ClientMessage(String msgId, String msgType, String timestamp, String pubKey, String signature, Transaction transaction) {
        super(msgId, msgType, timestamp, pubKey, signature);
        this.transaction = transaction;
    }

    @Override
    public String toString() {
        String rtn = null;
        try {
            rtn = (new ObjectMapper()).writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return rtn;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }
}
