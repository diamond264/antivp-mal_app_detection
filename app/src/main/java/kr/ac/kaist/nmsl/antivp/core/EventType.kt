package kr.ac.kaist.nmsl.antivp.core

enum class EventType(val value: Int) {
    CALL_RINGING(1),
    CALL_OFFHOOK(2),
    CALL_IDLE(3),
    SMS_RCVD(4),
    TEXT_TRANSCRIBED(5),
    PHISHING_CALL_DETECTED(6),
    PHISHING_APP_DETECTED(7),
    GET_OPTIMAL_MODEL(8),
    APP_DOWNLOADED(9),
    SMISHING_SMS_DETECTED(10),
    // For testing
    TEST_APP_DL(11),
    TEST_BERT_DATA_BROADCASTED(900),
    TEST_S2T_DATA_BROADCASTED(901),
    TEST_MAL_DATA_BROADCASTED(902),
    ;

    companion object {
        fun valueOf(value: Int): EventType? = EventType.values().find { it.value == value }
    }

}