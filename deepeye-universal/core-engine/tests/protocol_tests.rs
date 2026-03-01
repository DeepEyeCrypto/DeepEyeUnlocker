use deepeyecore::protocols::mtk::brom::BromProtocol;
use deepeyecore::protocols::qcom::sahara::{SaharaProtocol, SAHARA_HELLO_REQ};

#[test]
fn test_sahara_handshake_parse() {
    let protocol = SaharaProtocol::new();

    // Mock an ideal Sahara Hello Request packet (48 bytes minimum)
    let mut rx_buffer = vec![0u8; 48];
    rx_buffer[0..4].copy_from_slice(&SAHARA_HELLO_REQ.to_le_bytes()); // Command
    rx_buffer[4..8].copy_from_slice(&48u32.to_le_bytes()); // Packet Length
    rx_buffer[8..12].copy_from_slice(&2u32.to_le_bytes()); // Version
    rx_buffer[12..16].copy_from_slice(&1u32.to_le_bytes()); // Min Version
    rx_buffer[16..20].copy_from_slice(&1024u32.to_le_bytes()); // Max Packet Size
    rx_buffer[20..24].copy_from_slice(&0u32.to_le_bytes()); // Mode (0 = Image TX)

    let result = protocol.execute_handshake(&rx_buffer);
    assert!(result.is_ok(), "Failed to parse valid Sahara response");

    let info = result.unwrap();
    assert_eq!(info.version, 2);
    assert_eq!(info.max_cmd_packet_size, 1024);
}

#[test]
fn test_brom_handshake_parse() {
    let protocol = BromProtocol::new();

    // Mock an ideal MTK BROM Handshake packet (10 bytes)
    let mut rx_buffer = vec![0u8; 10];
    rx_buffer[0..2].copy_from_slice(&0x6768u16.to_be_bytes()); // BBChip (e.g. MT6768)
    rx_buffer[8..10].copy_from_slice(&0xCAFEu16.to_be_bytes()); // HW Ver

    let result = protocol.execute_handshake(&rx_buffer);
    assert!(result.is_ok(), "Failed to parse valid BROM response");

    let info = result.unwrap();
    assert_eq!(info.bbchip, 0x6768);
    assert_eq!(info.hw_ver, 0xCAFE);
}

#[test]
fn test_sahara_buffer_bounds() {
    let protocol = SaharaProtocol::new();
    let rx_buffer = vec![0u8; 16]; // Too small

    let result = protocol.execute_handshake(&rx_buffer);
    assert!(
        result.is_err(),
        "Sahara parsing did not catch undersized buffer"
    );
}
