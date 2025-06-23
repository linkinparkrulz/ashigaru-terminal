package com.samourai.wallet.segwit.bech32;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.samourai.wallet.util.Triple;

public class Bech32Segwit {

  public static Pair<Byte, byte[]> decode(String hrp, String addr)  {

      Triple<String, byte[], Integer> p = Bech32.bech32Decode(addr);
      if(p == null)  {
        return null;
      }

      String hrpgotStr =  p.getLeft();
      if(hrpgotStr == null)  {
        return null;
      }
      if (!hrp.equalsIgnoreCase(hrpgotStr))    {
        return null;
      }
      if (!hrpgotStr.equalsIgnoreCase("bc") && !hrpgotStr.equalsIgnoreCase("tb"))    {
        return null;
      }

      byte[] data = p.getMiddle();
      List<Byte> progBytes = new ArrayList<Byte>();
      for(int i = 1; i < data.length; i++) {
          progBytes.add(data[i]);
      }
      byte[] decoded = convertBits(progBytes, 5, 8, false);
      if(decoded.length < 2 || decoded.length > 40)   {
          return null;
      }

      byte witnessVersion = data[0];
      if (witnessVersion > (byte)16)   {
          return null;
      }

      if (witnessVersion == (byte)0x00 && decoded.length != 20 && decoded.length != 32)   {
          return null;
      }

      if ((witnessVersion == (byte)0x00 && p.getRight() != Bech32.BECH32) || (witnessVersion != (byte)0x00 && p.getRight() != Bech32.BECH32M))   {
          return null;
      }

      return Pair.of(witnessVersion, decoded);
  }

  public static String encode(String hrp, byte witnessVersion, byte[] witnessProgram) {

      int spec = ((witnessVersion == (byte)0x00) ? Bech32.BECH32 : Bech32.BECH32M);

      List<Byte> progBytes = new ArrayList<Byte>();
      for(int i = 0; i < witnessProgram.length; i++) {
          progBytes.add(witnessProgram[i]);
      }

      byte[] prog = convertBits(progBytes, 8, 5, true);
      byte[] data = new byte[1 + prog.length];

      System.arraycopy(new byte[] { witnessVersion }, 0, data, 0, 1);
      System.arraycopy(prog, 0, data, 1, prog.length);

      String ret = Bech32.bech32Encode(hrp, data, spec);
      if(ret == null)    {
          return null;
      }

      Pair<Byte, byte[]> pair = decode(hrp, ret);
      return pair == null ? null : ret;
  }

  private static byte[] convertBits(List<Byte> data, int fromBits, int toBits, boolean pad)  {

      int acc = 0;
      int bits = 0;
      int maxv = (1 << toBits) - 1;
      int max_acc = (1 << (fromBits + toBits - 1)) - 1;
      List<Byte> ret = new ArrayList<Byte>();

      for(Byte value : data)  {

          short b = (short)(value.byteValue() & 0xff);
          if (b < 0) {
            return null;
          }
          else if ((b >> fromBits) > 0) {
              return null;
          }
          else    {
              ;
          }

          acc = ((acc << fromBits) | b) & max_acc;
          bits += fromBits;
          while (bits >= toBits)  {
              bits -= toBits;
              ret.add((byte)((acc >> bits) & maxv));
          }
      }

      if(pad && (bits > 0))    {
          ret.add((byte)((acc << (toBits - bits)) & maxv));
      }
      else if (bits >= fromBits || (byte)(((acc << (toBits - bits)) & maxv)) != 0)    {
          return null;
      }
      else    {
          ;
      }

      byte[] buf = new byte[ret.size()];
      for(int i = 0; i < ret.size(); i++) {
          buf[i] = ret.get(i);
      }

      return buf;
  }

  public static byte[] getScriptPubkey(byte witver, byte[] witprog) {

      byte v = (witver > (byte)0x00) ? (byte)(witver + 0x50) : (byte)0x00;
      byte[] ver = new byte[] { v, (byte)witprog.length };

      byte[] ret = new byte[witprog.length + ver.length];
      System.arraycopy(ver, 0, ret, 0, ver.length);
      System.arraycopy(witprog, 0, ret, ver.length, witprog.length);

      return ret;
  }

}
