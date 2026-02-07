using System;

namespace DeepEyeUnlocker.Protocols.SPD
{
    public static class SpdConstants
    {
        // HDLC Framing
        public const byte HDLC_FLAG = 0x7E;
        public const byte HDLC_ESCAPE = 0x7D;
        public const byte HDLC_ESCAPE_MASK = 0x20;

        // Command Codes
        public const ushort BSL_CMD_CONNECT = 0x00;
        public const ushort BSL_CMD_START_DATA = 0x01;
        public const ushort BSL_CMD_MID_DATA = 0x02;
        public const ushort BSL_CMD_END_DATA = 0x03;
        public const ushort BSL_CMD_EXEC_DATA = 0x04;
        public const ushort BSL_CMD_READ_FLASH = 0x05;
        public const ushort BSL_CMD_CHANGE_BAUD = 0x09;
        public const ushort BSL_CMD_READ_CHIPID = 0x0B;
        public const ushort BSL_CMD_RESET = 0x0F;

        // Diag Command Codes
        public const ushort DIAG_CMD_GET_VERSION = 0x01;
        public const ushort DIAG_CMD_READ_NV = 0x11;
        public const ushort DIAG_CMD_WRITE_NV = 0x12;
        public const ushort DIAG_CMD_READ_INFO = 0x19;
        public const ushort DIAG_CMD_FACTORY_RESET = 0x1B;

        // Response Codes
        public const ushort BSL_REP_ACK = 0x80;
        public const ushort BSL_REP_NAK = 0x81;
        public const ushort BSL_REP_VER = 0x82;
        public const ushort BSL_REP_INVALID_CMD = 0x83;
        public const ushort BSL_REP_UNKNOW_ERROR = 0x84;
        public const ushort BSL_REP_INCOMPATIBLE_PARTITION = 0x85;
        public const ushort BSL_REP_WRONG_IP = 0x86;
        public const ushort BSL_REP_WRONG_SIZE = 0x87;
        public const ushort BSL_REP_VERIFY_ERROR = 0x88;
        public const ushort BSL_REP_NOT_VERIFIED = 0x89;

        // Chip IDs
        public const uint CHIP_SC9863A = 0x98630000;
        public const uint CHIP_SC7731E = 0x77310000;
    }
}
