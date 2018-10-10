package com.credenceid.sdkapp.models;

/* This class is used by CardReaderPage to transmit APDUs based on current card type connected.
 * Device will first attempt to determine card type, then use appropriate APDU to read data.
 */
@SuppressWarnings({"SpellCheckingInspection"})
public class CardDataTables {
	// Gemalto MPCOS EMV APDU table
	public static final String[] MPCOS_EMV_APDU_table = {
			"00a4000c023f00", "SELECT MF 3F00", // select master file 0x3f00
			"00a40100020100", "SELECT DF 0100", // select directory 0x0100
			"00a40200020101", "SELECT EF 0101", // select elementary file 0x0101
			"00b000000C", "READ BINARY",      // READ BINARY
	};

	// Payflex 1K APDU table
	public static final String[] Payflex_1K_APDU_table = {
			"00a40000020002", "SELECT FILE",
			"00b2000008", "READ RECORD",
	};

	// G&D Sm@rtCafe Expert 64
	public static final String[] GD_SmartCafe_Expert64_APDU_table = {
			"80CA00C3", "GET DATA",
	};

	// Oberthur ID One 128 v5.5 Dual CAC
	public static final String[] Oberthur_ID_One_128_v55_Dual_CAC_APDU_table = {
			"00a4040007a0000000790201", "SELECT GC1 Applet",
			"00a4040007a0000000790202", "SELECT GC2 Applet",
			"00a4040007a0000000030000", "SELECT CM",
			"80ca9f7f", "GET DATA",
	};

	// Oberthur ID One v5.2a Dual CAC
	public static final String[] Oberthur_ID_One_v52a_Dual_CAC_APDU_table = {
			"00a4040007a0000000790201", "SELECT GC1 Applet",
			"00a4040007a0000000790202", "SELECT GC2 Applet",
			"00a4040007a0000000030000", "SELECT CM",
			"80ca9f7f", "GET DATA",
	};

	// Gemalto GemXpresso 64K
	public static final String[] Gemalto_GemXpresso_64K_APDU_table = {
			"00a4040007a000000018434d", "SELECT CM",
			"80ca9f7f2d", "GET DATA",
	};

	// Pakistan eID
	public static final String[] Pakistan_eID_APDU_table = {
			"00A404000EA0000005176964656E7469747901", "SELECT eID Applet",
			"EE490000", "READ Citizen ID",
	};

	public static final String[] UAE_eID_APDU_table = {
			"00a404000CA00000024300130000000101", "SELECT eID Applet",
			"EE490000", "READ Citizen ID",
	};

	// US Passport
	public static final String[] US_ePassport_APDU_table = {
			"00A4040C07A0000002471001", "Select eID Applet",
			"0084000008", "GET DATA",
	};

	// MIFARE
	public static final String[] MiFARE_APDU_table = {
			"FFB00000000800", "Read 2K ",
			"FFB00000001000", "Read 4K",
	};

	// NULL (no card identified) APDU table
	public static String[] noAPDU_table = {"", "No APDUs Defined"};
}
