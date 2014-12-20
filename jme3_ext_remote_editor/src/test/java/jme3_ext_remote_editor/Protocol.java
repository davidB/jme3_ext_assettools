package jme3_ext_remote_editor;

public class Protocol {
	public static class Kind {
		public static byte pingpong = 0x01;
		public static byte logs = 0x02;
		public static byte askScreenshot = 0x03;
		public static byte rawScreenshot = 0x04;
		public static byte msgpack = 0x05;
		public static byte pgex_cmd = 0x06;
	}
}
