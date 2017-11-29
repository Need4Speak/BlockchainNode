package service;

import com.fasterxml.jackson.databind.ObjectMapper;
import entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.MongoUtil;
import util.SignatureUtil;
import util.TimeUtil;

import java.io.IOException;
import java.security.PrivateKey;
import java.util.Map;

import static service.TransactionService.genTx;
import static util.SignatureUtil.getSha256Base64;
import static util.SignatureUtil.loadPubKeyStr;
import static util.SignatureUtil.loadPvtKey;

/**
 * Created by chao on 2017/11/21.
 */
public class MessageService {
    private final static Logger logger = LoggerFactory.getLogger(TransactionService.class);
    private final static ObjectMapper objectMapper = new ObjectMapper();
    /**
     * 根据 msgType 和 transaction 生成 message
     *
     * @param msgType
     * @param transaction
     * @return
     */
    public static ClientMessage genTxMsg(String msgType, Transaction transaction) {
        String timestamp = TimeUtil.getNowTimeStamp();
        PrivateKey privateKey = loadPvtKey("EC");
        String pubKey = loadPubKeyStr("EC");
        String signature = SignatureUtil.sign(privateKey, transaction.toString());
        String msgId = getSha256Base64(signature);
        return new ClientMessage(msgId, msgType, timestamp, pubKey, signature, transaction);
    }

    /**
     * 将 ClientMessage 对象存入集合 collectionName 中。
     * @param cliMsg
     * @param collectionName
     * @return
     */
    public static boolean saveCliMsg(ClientMessage cliMsg, String collectionName){
        if(MongoUtil.findByKV("msgId", cliMsg.getMsgId(), collectionName)) {
            logger.info("cliMsg 消息 [" + cliMsg.getMsgId() + "] 已存在");
            return false;
        } else {
            MongoUtil.insertJson(cliMsg.toString(), collectionName);
        }
        return false;
    }

    /**
     * 将 ClientMessage json 字符串存入集合 collectionName 中。
     * @param cliMsgStr
     * @param collectionName
     * @return
     */
    public static boolean saveCliMsg(String cliMsgStr, String collectionName){
        ClientMessage cliMsg = null;
        try {
            cliMsg = objectMapper.readValue(cliMsgStr, ClientMessage.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return saveCliMsg(cliMsg, collectionName);
    }

    public static boolean savePPMsg(PrePrepareMessage ppMsg, String collectionName){
        if(MongoUtil.findByKV("msgId", ppMsg.getMsgId(), collectionName)) {
            logger.info("ppMsg 消息 [" + ppMsg.getMsgId() + "] 已存在");
            return false;
        } else {
            MongoUtil.insertJson(ppMsg.toString(), collectionName);
        }
        return false;
    }

    public static boolean savePPMsg(String ppMsgStr, String collectionName){
        ClientMessage cliMsg = null;
        try {
            cliMsg = objectMapper.readValue(ppMsgStr, ClientMessage.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return saveCliMsg(cliMsg, collectionName);
    }

    public static PrePrepareMessage genPrePrepareMsg(String seqNum, String cliMsgId) {
        String timestamp = TimeUtil.getNowTimeStamp();
        String viewId = "1";

        PrivateKey privateKey = loadPvtKey("EC");
        String pubKey = loadPubKeyStr("EC");
        String signature = SignatureUtil.sign(privateKey, getPPMSignContent(cliMsgId, viewId, seqNum, timestamp));
        String msgId = getSha256Base64(signature);
        return new PrePrepareMessage(msgId, timestamp, pubKey, signature, viewId, seqNum, cliMsgId);
    }

    /**
     * 检验 PrePrepareMessage的正确性
     *
     * @param ppm
     * @return
     */
    public static boolean verifyPrePrepareMsg(PrePrepareMessage ppm) {
        if (!SignatureUtil.verify(ppm.getPubKey(), getPPMSignContent(ppm.getCliMsgId(), ppm.getViewId(),
                ppm.getSeqNum(), ppm.getTimestamp()), ppm.getSignature())) {
            return false;
        }
        return true;
    }

    /**
     * 根据传入的内容生成 ppm 要签名的字符串
     * @param cliMsgId
     * @param viewId
     * @param seqNum
     * @param timestamp
     * @return
     */
    public static String getPPMSignContent(String cliMsgId, String viewId, String seqNum, String timestamp) {
        StringBuilder sb = new StringBuilder();
        sb.append(cliMsgId).append(viewId).append(seqNum).append(timestamp);
        return sb.toString();
    }

    public static PrepareMessage genPrepareMsg(String msgType, String ip, int port) {
        String url = ip + ":" + port;
//        return new PrepareMessage(genMessage(msgType, url), "1", "1", ip, port);
        // TODO
        return null;
    }

    /**
     * 生成 Message 类的对象
     * @param msgType
     * @param sigContent
     * @return
     */
    public static Message genMessage(String msgType, String sigContent) {
        String timestamp = TimeUtil.getNowTimeStamp();
        PrivateKey privateKey = loadPvtKey("EC");
        String pubKey = loadPubKeyStr("EC");
        String signature = SignatureUtil.sign(privateKey, sigContent);
        String msgId = getSha256Base64(signature);
        return new Message(msgId, msgType, timestamp, pubKey, signature);
    }

    /**
     * 到 collectionName 里去获取当前的序列号
     * @param collectionName
     * @return
     * @throws Exception
     */
    public static long getSeqNum(String collectionName) throws Exception {
        if(!MongoUtil.collectionExists(collectionName)) {
            logger.info("集合" + collectionName + "不存在，开始创建");
            MongoUtil.insertKV("seqNum", "0", collectionName);
            return 0;
        } else {
            String record = MongoUtil.findFirstDoc(collectionName);
            if(record != null && !record.equals("")) {
                long seqNum = -1;
                try {
                    seqNum = Long.parseLong((String) objectMapper.readValue(record, Map.class).get("seqNum"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return seqNum;

            }
            else {
                throw new Exception("获取 seqNum 失败！");
            }
        }
    }

    /**
     * 到 collectionName 里去获取更新的序列号
     * @param collectionName
     * @throws Exception
     */
    public static long updateSeqNum(String collectionName) throws Exception {

        long oldSeqNum = getSeqNum(collectionName);
        long newSeqNum = oldSeqNum + 1;
        MongoUtil.updateKV("seqNum", Long.toString(oldSeqNum), Long.toString(newSeqNum), collectionName);
        return newSeqNum;
    }

    public static void main(String[] args) {
        try {
            ClientMessage txMsg = genTxMsg("cliMsg", genTx("string", "测试"));
            logger.info(txMsg.toString());
            PrePrepareMessage ppm = genPrePrepareMsg("1", txMsg.getTransaction().getTxId());
            logger.info(ppm.toString());
            logger.info(verifyPrePrepareMsg(ppm) + "");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
