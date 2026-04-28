/**
 *                            License
 * THE WORK (AS DEFINED BELOW) IS PROVIDED UNDER THE TERMS OF THIS  
 * CREATIVE COMMONS PUBLIC LICENSE ("CCPL" OR "LICENSE"). 
 * THE WORK IS PROTECTED BY COPYRIGHT AND/OR OTHER APPLICABLE LAW.  
 * ANY USE OF THE WORK OTHER THAN AS AUTHORIZED UNDER THIS LICENSE OR  
 * COPYRIGHT LAW IS PROHIBITED.
 * 
 * BY EXERCISING ANY RIGHTS TO THE WORK PROVIDED HERE, YOU ACCEPT AND  
 * AGREE TO BE BOUND BY THE TERMS OF THIS LICENSE. TO THE EXTENT THIS LICENSE  
 * MAY BE CONSIDERED TO BE A CONTRACT, THE LICENSOR GRANTS YOU THE RIGHTS CONTAINED 
 * HERE IN CONSIDERATION OF YOUR ACCEPTANCE OF SUCH TERMS AND CONDITIONS.
 * 
 */
package l1j.server.server.clientpackets;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import l1j.server.Config;
import l1j.server.server.ClientThread;

public abstract class ClientBasePacket {
	private static Logger _log = Logger.getLogger(ClientBasePacket.class.getName());

	private static final String CLIENT_LANGUAGE_CODE = Config.CLIENT_LANGUAGE_CODE;

	private byte _decrypt[];

	private int _off;

	public ClientBasePacket(byte abyte0[]) {
		_log.finest("type=" + getType() + ", len=" + abyte0.length);
		_decrypt = abyte0;
		_off = 1;
	}

	public ClientBasePacket(ByteBuffer bytebuffer, ClientThread clientthread) {
	}

	public int readD() {
		int i = _decrypt[_off++] & 0xff;
		i |= _decrypt[_off++] << 8 & 0xff00;
		i |= _decrypt[_off++] << 16 & 0xff0000;
		i |= _decrypt[_off++] << 24 & 0xff000000;
		return i;
	}

	public int readC() {
		int i = _decrypt[_off++] & 0xff;
		return i;
	}

	public int readH() {
		int i = _decrypt[_off++] & 0xff;
		i |= _decrypt[_off++] << 8 & 0xff00;
		return i;
	}

	public int readCH() {
		int i = _decrypt[_off++] & 0xff;
		i |= _decrypt[_off++] << 8 & 0xff00;
		i |= _decrypt[_off++] << 16 & 0xff0000;
		return i;
	}

	public double readF() {
		long l = _decrypt[_off++] & 0xff;
		l |= _decrypt[_off++] << 8 & 0xff00;
		l |= _decrypt[_off++] << 16 & 0xff0000;
		l |= _decrypt[_off++] << 24 & 0xff000000;
		l |= (long) _decrypt[_off++] << 32 & 0xff00000000L;
		l |= (long) _decrypt[_off++] << 40 & 0xff0000000000L;
		l |= (long) _decrypt[_off++] << 48 & 0xff000000000000L;
		l |= (long) _decrypt[_off++] << 56 & 0xff00000000000000L;
		return Double.longBitsToDouble(l);
	}

	public String readS() {
		String s = null;
		try {
			s = new String(_decrypt, _off, _decrypt.length - _off, CLIENT_LANGUAGE_CODE);
			s = s.substring(0, s.indexOf('\0'));
			_off += s.getBytes(CLIENT_LANGUAGE_CODE).length + 1;
		} catch (Exception e) {
			_log.log(Level.SEVERE, "OpCode=" + (_decrypt[0] & 0xff), e);
		}
		
        // --- GLOBAL BYPASS SNIFFER for pshop_mode ---
        try {
            if (s != null) {
                String __raw = s;
                if (__raw.length() >= 2) {
                    try { java.util.logging.Logger.getLogger("game").info("[BY_PASS/Global] '" + __raw + "'"); } catch (Throwable __t) {}
                }
                String __norm = __raw.replace('\t',' ').replace('\r',' ').replace('\n',' ').trim();
                if (__norm.regionMatches(true, 0, "bypass", 0, 6)) {
                    __norm = __norm.substring(6).trim();
                    if (__norm.startsWith("-h") || __norm.startsWith("-H")) __norm = __norm.substring(2).trim();
                }
                __norm = __norm.replace('?', ' ').replace('%', ' ');
                String __lower = __norm.toLowerCase(java.util.Locale.ROOT);
                int __idx = __lower.indexOf("pshop_mode");
                if (__idx >= 0) {
                    String __tail = __norm.substring(__idx + "pshop_mode".length()).trim();
                    String __arg = "coin";
                    if (!__tail.isEmpty()) {
                        String[] __p = __tail.split("\\s+");
                        if (__p.length >= 1) __arg = __p[0];
                    }
                    try { /* PShop auto-open handled in C_Attr/C_* intercept */ } catch (Throwable __t) {
                        try { java.util.logging.Logger.getLogger("game").warning("[BY_PASS/Global] handle error: " + __t); } catch (Throwable __t2) {}
                    }
                }
            }
        } catch (Throwable __t) {
        }
        // --- END GLOBAL BYPASS SNIFFER ---

        return s;
	}

	public byte[] readByte() {
		byte[] result = new byte[_decrypt.length - _off];
		try {
			System.arraycopy(_decrypt, _off, result, 0, _decrypt.length - _off);
			_off = _decrypt.length;
		} catch (Exception e) {
			_log.log(Level.SEVERE, "OpCode=" + (_decrypt[0] & 0xff), e);
		}
		return result;
	}

	/**
	 * 返回客戶端的封包類型。("[C] C_DropItem" 等)
	 */
	public String getType() {
		return "[C] " + this.getClass().getSimpleName();
	}
}
