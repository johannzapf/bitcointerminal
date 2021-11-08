package de.johannzapf.bitcoin.terminal.service;

import apdu4j.pcsc.SCard;
import de.johannzapf.bitcoin.terminal.util.Util;

import javax.smartcardio.Card;
import javax.smartcardio.CardException;
import java.nio.ByteBuffer;

import static de.johannzapf.bitcoin.terminal.util.Constants.*;

public class PINService {

    private static int CONTROL_CODE_VERIFY;
    private static int CONTROL_CODE_MODIFY;

    public static void parseControlCodes(Card card) throws CardException {
        int CONTROL_CODE_GET_FEATURES = SCard.CARD_CTL_CODE(3400);
        byte[] res = card.transmitControlCommand(CONTROL_CODE_GET_FEATURES, new byte[0]);
        for(int i = 0; i < res.length; i++){
            if(res[i] == 0x06){
                CONTROL_CODE_VERIFY = ByteBuffer.wrap(res, i+2, 4).getInt();
            }
            if(res[i] == 0x07){
                CONTROL_CODE_MODIFY = ByteBuffer.wrap(res, i+2, 4).getInt();
            }
        }
    }

    public static byte[] verifyPin(Card card) throws CardException {
        byte[] command = new byte[]{
                0x00, //Timeout
                0x00, //Timeout
                0x00, //Format
                0x0f, //PIN Block
                0x00, //PIN length format
                0x04, //Max PIN size
                0x04, //Min PIN size
                0x02, //Entry validation condition (02 = press OK)
                0x01, //Number of messages
                0x04, //Language
                0x09, //Language
                0x00, //Message index
                0x00, //TeoPrologue
                0x00, 0x00,
                0x04, 0x00, 0x00, 0x00, //APDU length
                (byte) CLA, INS_VERIFY_PIN, 0x00, 0x00};
        return card.transmitControlCommand(CONTROL_CODE_VERIFY, command);
    }

    public static byte[] modifyPin(Card card) throws CardException {
        byte[] command = new byte[]{
                0x00, //Timeout
                0x00, //Timeout
                0x00, //Format
                0x0f, //PIN Block
                0x00, //PIN length format
                0x00, //Offset for old PIN
                0x04, //Offset for new PIN
                0x04, //Max PIN size
                0x04, //Min PIN size
                0x02, //Confirmation (2 = Old PIN + new PIN twice)
                0x02, //Entry validation condition (2 = press OK)
                (byte)0xff, //Number of messages
                0x04, //Language
                0x09, //Language
                0x00, //Message index 1
                0x00, //Message index 2
                0x00, //Message index 3
                0x00, //TeoPrologue
                0x00, 0x00,
                0x04, 0x00, 0x00, 0x00, //APDU length
                (byte) CLA, INS_MODIFY_PIN, 0x00, 0x00};
        return card.transmitControlCommand(CONTROL_CODE_MODIFY, command);
    }
}
