package net.sourceforge.jasymcaandroid.jasymca;

public class BigInteger
{
 char Character_forDigit(int digit,
                            int radix){
  if(digit<0 || digit>= radix)
   return '\u0000';
  if(digit<10)
   return (char)(digit+'0');
  return (char) (digit-10+'a');
 }
  private transient int ival;
  private transient int[] words;
  private int bitCount = -1;
  private int bitLength = -1;
  private int firstNonzeroByteNum = -2;
  private int lowestSetBit = -2;
  private byte[] magnitude;
  private int signum;
  private static final long serialVersionUID = -8287574255936472291L;
  private static final int minFixNum = 0;
  private static final int maxFixNum = 10;
  private static final int numFixNum = maxFixNum-minFixNum+1;
  private static final BigInteger[] smallFixNums = new BigInteger[numFixNum];
  static
  {
    for (int i = numFixNum; --i >= 0; )
      smallFixNums[i] = new BigInteger(i + minFixNum);
  }
  public static final BigInteger ZERO = smallFixNums[0 - minFixNum];
  public static final BigInteger ONE = smallFixNums[1 - minFixNum];
  public static final BigInteger TEN = smallFixNums[10 - minFixNum];
  private static final int FLOOR = 1;
  private static final int CEILING = 2;
  private static final int TRUNCATE = 3;
  private static final int ROUND = 4;
  private static final int[] primes =
    { 2, 3, 5, 7 };
  private static final int[] k =
      {100,150,200,250,300,350,400,500,600,800,1250, Integer.MAX_VALUE};
  private static final int[] t =
      { 27, 18, 15, 12, 9, 8, 7, 6, 5, 4, 3, 2};
  private BigInteger()
  {
  }
  private BigInteger(int value)
  {
    ival = value;
  }
  public BigInteger(String val, int radix)
  {
    BigInteger result = valueOf(val, radix);
    this.ival = result.ival;
    this.words = result.words;
  }
  public BigInteger(String val)
  {
    this(val, 10);
  }
  public BigInteger(byte[] val)
  {
    if (val == null || val.length < 1)
      throw new NumberFormatException();
    words = byteArrayToIntArray(val, val[0] < 0 ? -1 : 0);
    BigInteger result = make(words, words.length);
    this.ival = result.ival;
    this.words = result.words;
  }
  public BigInteger(int signum, byte[] magnitude)
  {
    if (magnitude == null || signum > 1 || signum < -1)
      throw new NumberFormatException();
    if (signum == 0)
      {
 int i;
 for (i = magnitude.length - 1; i >= 0 && magnitude[i] == 0; --i)
   ;
 if (i >= 0)
   throw new NumberFormatException();
        return;
      }
    words = byteArrayToIntArray(magnitude, 0);
    BigInteger result = make(words, words.length);
    this.ival = result.ival;
    this.words = result.words;
    if (signum < 0)
      setNegative();
  }
  public static BigInteger valueOf(long val)
  {
    if (val >= minFixNum && val <= maxFixNum)
      return smallFixNums[(int) val - minFixNum];
    int i = (int) val;
    if ((long) i == val)
      return new BigInteger(i);
    BigInteger result = alloc(2);
    result.ival = 2;
    result.words[0] = i;
    result.words[1] = (int)(val >> 32);
    return result;
  }
  private static BigInteger make(int[] words, int len)
  {
    if (words == null)
      return valueOf(len);
    len = BigInteger.wordsNeeded(words, len);
    if (len <= 1)
      return len == 0 ? ZERO : valueOf(words[0]);
    BigInteger num = new BigInteger();
    num.words = words;
    num.ival = len;
    return num;
  }
  private static int[] byteArrayToIntArray(byte[] bytes, int sign)
  {
    int[] words = new int[bytes.length/4 + 1];
    int nwords = words.length;
    int bptr = 0;
    int word = sign;
    for (int i = bytes.length % 4; i > 0; --i, bptr++)
      word = (word << 8) | (bytes[bptr] & 0xff);
    words[--nwords] = word;
    while (nwords > 0)
      words[--nwords] = bytes[bptr++] << 24 |
   (bytes[bptr++] & 0xff) << 16 |
   (bytes[bptr++] & 0xff) << 8 |
   (bytes[bptr++] & 0xff);
    return words;
  }
  private static BigInteger alloc(int nwords)
  {
    BigInteger result = new BigInteger();
    if (nwords > 1)
    result.words = new int[nwords];
    return result;
  }
  private void realloc(int nwords)
  {
    if (nwords == 0)
      {
 if (words != null)
   {
     if (ival > 0)
       ival = words[0];
     words = null;
   }
      }
    else if (words == null
      || words.length < nwords
      || words.length > nwords + 2)
      {
 int[] new_words = new int [nwords];
 if (words == null)
   {
     new_words[0] = ival;
     ival = 1;
   }
 else
   {
     if (nwords < ival)
       ival = nwords;
     System.arraycopy(words, 0, new_words, 0, ival);
   }
 words = new_words;
      }
  }
  private boolean isNegative()
  {
    return (words == null ? ival : words[ival - 1]) < 0;
  }
  public int signum()
  {
    int top = words == null ? ival : words[ival-1];
    if (top == 0 && words == null)
      return 0;
    return top < 0 ? -1 : 1;
  }
  private static int compareTo(BigInteger x, BigInteger y)
  {
    if (x.words == null && y.words == null)
      return x.ival < y.ival ? -1 : x.ival > y.ival ? 1 : 0;
    boolean x_negative = x.isNegative();
    boolean y_negative = y.isNegative();
    if (x_negative != y_negative)
      return x_negative ? -1 : 1;
    int x_len = x.words == null ? 1 : x.ival;
    int y_len = y.words == null ? 1 : y.ival;
    if (x_len != y_len)
      return (x_len > y_len) != x_negative ? 1 : -1;
    return MPN.cmp(x.words, y.words, x_len);
  }
  public int compareTo(BigInteger val)
  {
    return compareTo(this, val);
  }
  public BigInteger min(BigInteger val)
  {
    return compareTo(this, val) < 0 ? this : val;
  }
  public BigInteger max(BigInteger val)
  {
    return compareTo(this, val) > 0 ? this : val;
  }
  private boolean isZero()
  {
    return words == null && ival == 0;
  }
  private boolean isOne()
  {
    return words == null && ival == 1;
  }
  private static int wordsNeeded(int[] words, int len)
  {
    int i = len;
    if (i > 0)
      {
 int word = words[--i];
 if (word == -1)
   {
     while (i > 0 && (word = words[i - 1]) < 0)
       {
  i--;
  if (word != -1) break;
       }
   }
 else
   {
     while (word == 0 && i > 0 && (word = words[i - 1]) >= 0) i--;
   }
      }
    return i + 1;
  }
  private BigInteger canonicalize()
  {
    if (words != null
 && (ival = BigInteger.wordsNeeded(words, ival)) <= 1)
      {
 if (ival == 1)
   ival = words[0];
 words = null;
      }
    if (words == null && ival >= minFixNum && ival <= maxFixNum)
      return smallFixNums[ival - minFixNum];
    return this;
  }
  private static BigInteger add(int x, int y)
  {
    return valueOf((long) x + (long) y);
  }
  private static BigInteger add(BigInteger x, int y)
  {
    if (x.words == null)
      return BigInteger.add(x.ival, y);
    BigInteger result = new BigInteger(0);
    result.setAdd(x, y);
    return result.canonicalize();
  }
  private void setAdd(BigInteger x, int y)
  {
    if (x.words == null)
      {
 set((long) x.ival + (long) y);
 return;
      }
    int len = x.ival;
    realloc(len + 1);
    long carry = y;
    for (int i = 0; i < len; i++)
      {
 carry += ((long) x.words[i] & 0xffffffffL);
 words[i] = (int) carry;
 carry >>= 32;
      }
    if (x.words[len - 1] < 0)
      carry--;
    words[len] = (int) carry;
    ival = wordsNeeded(words, len + 1);
  }
  private void setAdd(int y)
  {
    setAdd(this, y);
  }
  private void set(long y)
  {
    int i = (int) y;
    if ((long) i == y)
      {
 ival = i;
 words = null;
      }
    else
      {
 realloc(2);
 words[0] = i;
 words[1] = (int) (y >> 32);
 ival = 2;
      }
  }
  private void set(int[] words, int length)
  {
    this.ival = length;
    this.words = words;
  }
  private void set(BigInteger y)
  {
    if (y.words == null)
      set(y.ival);
    else if (this != y)
      {
 realloc(y.ival);
 System.arraycopy(y.words, 0, words, 0, y.ival);
 ival = y.ival;
      }
  }
  private static BigInteger add(BigInteger x, BigInteger y, int k)
  {
    if (x.words == null && y.words == null)
      return valueOf((long) k * (long) y.ival + (long) x.ival);
    if (k != 1)
      {
 if (k == -1)
   y = BigInteger.neg(y);
 else
   y = BigInteger.times(y, valueOf(k));
      }
    if (x.words == null)
      return BigInteger.add(y, x.ival);
    if (y.words == null)
      return BigInteger.add(x, y.ival);
    if (y.ival > x.ival)
      {
 BigInteger tmp = x; x = y; y = tmp;
      }
    BigInteger result = alloc(x.ival + 1);
    int i = y.ival;
    long carry = MPN.add_n(result.words, x.words, y.words, i);
    long y_ext = y.words[i - 1] < 0 ? 0xffffffffL : 0;
    for (; i < x.ival; i++)
      {
 carry += ((long) x.words[i] & 0xffffffffL) + y_ext;;
 result.words[i] = (int) carry;
 carry >>>= 32;
      }
    if (x.words[i - 1] < 0)
      y_ext--;
    result.words[i] = (int) (carry + y_ext);
    result.ival = i+1;
    return result.canonicalize();
  }
  public BigInteger add(BigInteger val)
  {
    return add(this, val, 1);
  }
  public BigInteger subtract(BigInteger val)
  {
    return add(this, val, -1);
  }
  private static BigInteger times(BigInteger x, int y)
  {
    if (y == 0)
      return ZERO;
    if (y == 1)
      return x;
    int[] xwords = x.words;
    int xlen = x.ival;
    if (xwords == null)
      return valueOf((long) xlen * (long) y);
    boolean negative;
    BigInteger result = BigInteger.alloc(xlen + 1);
    if (xwords[xlen - 1] < 0)
      {
 negative = true;
 negate(result.words, xwords, xlen);
 xwords = result.words;
      }
    else
      negative = false;
    if (y < 0)
      {
 negative = !negative;
 y = -y;
      }
    result.words[xlen] = MPN.mul_1(result.words, xwords, xlen, y);
    result.ival = xlen + 1;
    if (negative)
      result.setNegative();
    return result.canonicalize();
  }
  private static BigInteger times(BigInteger x, BigInteger y)
  {
    if (y.words == null)
      return times(x, y.ival);
    if (x.words == null)
      return times(y, x.ival);
    boolean negative = false;
    int[] xwords;
    int[] ywords;
    int xlen = x.ival;
    int ylen = y.ival;
    if (x.isNegative())
      {
 negative = true;
 xwords = new int[xlen];
 negate(xwords, x.words, xlen);
      }
    else
      {
 negative = false;
 xwords = x.words;
      }
    if (y.isNegative())
      {
 negative = !negative;
 ywords = new int[ylen];
 negate(ywords, y.words, ylen);
      }
    else
      ywords = y.words;
    if (xlen < ylen)
      {
 int[] twords = xwords; xwords = ywords; ywords = twords;
 int tlen = xlen; xlen = ylen; ylen = tlen;
      }
    BigInteger result = BigInteger.alloc(xlen+ylen);
    MPN.mul(result.words, xwords, xlen, ywords, ylen);
    result.ival = xlen+ylen;
    if (negative)
      result.setNegative();
    return result.canonicalize();
  }
  public BigInteger multiply(BigInteger y)
  {
    return times(this, y);
  }
  private static void divide(long x, long y,
        BigInteger quotient, BigInteger remainder,
        int rounding_mode)
  {
    boolean xNegative, yNegative;
    if (x < 0)
      {
 xNegative = true;
 if (x == Long.MIN_VALUE)
   {
     divide(valueOf(x), valueOf(y),
     quotient, remainder, rounding_mode);
     return;
   }
 x = -x;
      }
    else
      xNegative = false;
    if (y < 0)
      {
 yNegative = true;
 if (y == Long.MIN_VALUE)
   {
     if (rounding_mode == TRUNCATE)
       {
  if (quotient != null)
    quotient.set(0);
  if (remainder != null)
    remainder.set(x);
       }
     else
       divide(valueOf(x), valueOf(y),
        quotient, remainder, rounding_mode);
     return;
   }
 y = -y;
      }
    else
      yNegative = false;
    long q = x / y;
    long r = x % y;
    boolean qNegative = xNegative ^ yNegative;
    boolean add_one = false;
    if (r != 0)
      {
 switch (rounding_mode)
   {
   case TRUNCATE:
     break;
   case CEILING:
   case FLOOR:
     if (qNegative == (rounding_mode == FLOOR))
       add_one = true;
     break;
   case ROUND:
     add_one = r > ((y - (q & 1)) >> 1);
     break;
   }
      }
    if (quotient != null)
      {
 if (add_one)
   q++;
 if (qNegative)
   q = -q;
 quotient.set(q);
      }
    if (remainder != null)
      {
 if (add_one)
   {
     r = y - r;
     xNegative = ! xNegative;
   }
 else
   {
   }
 if (xNegative)
   r = -r;
 remainder.set(r);
      }
  }
  private static void divide(BigInteger x, BigInteger y,
        BigInteger quotient, BigInteger remainder,
        int rounding_mode)
  {
    if ((x.words == null || x.ival <= 2)
 && (y.words == null || y.ival <= 2))
      {
 long x_l = x.longValue();
 long y_l = y.longValue();
 if (x_l != Long.MIN_VALUE && y_l != Long.MIN_VALUE)
   {
     divide(x_l, y_l, quotient, remainder, rounding_mode);
     return;
   }
      }
    boolean xNegative = x.isNegative();
    boolean yNegative = y.isNegative();
    boolean qNegative = xNegative ^ yNegative;
    int ylen = y.words == null ? 1 : y.ival;
    int[] ywords = new int[ylen];
    y.getAbsolute(ywords);
    while (ylen > 1 && ywords[ylen - 1] == 0) ylen--;
    int xlen = x.words == null ? 1 : x.ival;
    int[] xwords = new int[xlen+2];
    x.getAbsolute(xwords);
    while (xlen > 1 && xwords[xlen-1] == 0) xlen--;
    int qlen, rlen;
    int cmpval = MPN.cmp(xwords, xlen, ywords, ylen);
    if (cmpval < 0)
      {
 int[] rwords = xwords; xwords = ywords; ywords = rwords;
 rlen = xlen; qlen = 1; xwords[0] = 0;
      }
    else if (cmpval == 0)
      {
 xwords[0] = 1; qlen = 1;
 ywords[0] = 0; rlen = 1;
      }
    else if (ylen == 1)
      {
 qlen = xlen;
 if (ywords[0] == 1 && xwords[xlen-1] < 0)
   qlen++;
 rlen = 1;
 ywords[0] = MPN.divmod_1(xwords, xwords, xlen, ywords[0]);
      }
    else
      {
 int nshift = MPN.count_leading_zeros(ywords[ylen - 1]);
 if (nshift != 0)
   {
     MPN.lshift(ywords, 0, ywords, ylen, nshift);
     int x_high = MPN.lshift(xwords, 0, xwords, xlen, nshift);
     xwords[xlen++] = x_high;
   }
 if (xlen == ylen)
   xwords[xlen++] = 0;
 MPN.divide(xwords, xlen, ywords, ylen);
 rlen = ylen;
 MPN.rshift0 (ywords, xwords, 0, rlen, nshift);
 qlen = xlen + 1 - ylen;
 if (quotient != null)
   {
     for (int i = 0; i < qlen; i++)
       xwords[i] = xwords[i+ylen];
   }
      }
    if (ywords[rlen-1] < 0)
      {
        ywords[rlen] = 0;
        rlen++;
      }
    boolean add_one = false;
    if (rlen > 1 || ywords[0] != 0)
      {
 switch (rounding_mode)
   {
   case TRUNCATE:
     break;
   case CEILING:
   case FLOOR:
     if (qNegative == (rounding_mode == FLOOR))
       add_one = true;
     break;
   case ROUND:
     BigInteger tmp = remainder == null ? new BigInteger() : remainder;
     tmp.set(ywords, rlen);
     tmp = shift(tmp, 1);
     if (yNegative)
       tmp.setNegative();
     int cmp = compareTo(tmp, y);
     if (yNegative)
       cmp = -cmp;
     add_one = (cmp == 1) || (cmp == 0 && (xwords[0]&1) != 0);
   }
      }
    if (quotient != null)
      {
 quotient.set(xwords, qlen);
 if (qNegative)
   {
     if (add_one)
       quotient.setInvert();
     else
       quotient.setNegative();
   }
 else if (add_one)
   quotient.setAdd(1);
      }
    if (remainder != null)
      {
 remainder.set(ywords, rlen);
 if (add_one)
   {
     BigInteger tmp;
     if (y.words == null)
       {
  tmp = remainder;
  tmp.set(yNegative ? ywords[0] + y.ival : ywords[0] - y.ival);
       }
     else
       tmp = BigInteger.add(remainder, y, yNegative ? 1 : -1);
     if (xNegative)
       remainder.setNegative(tmp);
     else
       remainder.set(tmp);
   }
 else
   {
     if (xNegative)
       remainder.setNegative();
   }
      }
  }
  public BigInteger divide(BigInteger val)
  {
    if (val.isZero())
      throw new ArithmeticException("divisor is zero");
    BigInteger quot = new BigInteger();
    divide(this, val, quot, null, TRUNCATE);
    return quot.canonicalize();
  }
  public BigInteger remainder(BigInteger val)
  {
    if (val.isZero())
      throw new ArithmeticException("divisor is zero");
    BigInteger rem = new BigInteger();
    divide(this, val, null, rem, TRUNCATE);
    return rem.canonicalize();
  }
  public BigInteger[] divideAndRemainder(BigInteger val)
  {
    if (val.isZero())
      throw new ArithmeticException("divisor is zero");
    BigInteger[] result = new BigInteger[2];
    result[0] = new BigInteger();
    result[1] = new BigInteger();
    divide(this, val, result[0], result[1], TRUNCATE);
    result[0].canonicalize();
    result[1].canonicalize();
    return result;
  }
  public BigInteger mod(BigInteger m)
  {
    if (m.isNegative() || m.isZero())
      throw new ArithmeticException("non-positive modulus");
    BigInteger rem = new BigInteger();
    divide(this, m, null, rem, FLOOR);
    return rem.canonicalize();
  }
  public BigInteger pow(int exponent)
  {
    if (exponent <= 0)
      {
 if (exponent == 0)
   return ONE;
   throw new ArithmeticException("negative exponent");
      }
    if (isZero())
      return this;
    int plen = words == null ? 1 : ival;
    int blen = ((bitLength() * exponent) >> 5) + 2 * plen;
    boolean negative = isNegative() && (exponent & 1) != 0;
    int[] pow2 = new int [blen];
    int[] rwords = new int [blen];
    int[] work = new int [blen];
    getAbsolute(pow2);
    int rlen = 1;
    rwords[0] = 1;
    for (;;)
      {
 if ((exponent & 1) != 0)
   {
     MPN.mul(work, pow2, plen, rwords, rlen);
     int[] temp = work; work = rwords; rwords = temp;
     rlen += plen;
     while (rwords[rlen - 1] == 0) rlen--;
   }
 exponent >>= 1;
 if (exponent == 0)
   break;
 MPN.mul(work, pow2, plen, pow2, plen);
 int[] temp = work; work = pow2; pow2 = temp;
 plen *= 2;
 while (pow2[plen - 1] == 0) plen--;
      }
    if (rwords[rlen - 1] < 0)
      rlen++;
    if (negative)
      negate(rwords, rwords, rlen);
    return BigInteger.make(rwords, rlen);
  }
  private static int[] euclidInv(int a, int b, int prevDiv)
  {
    if (b == 0)
      throw new ArithmeticException("not invertible");
    if (b == 1)
 return new int[] { -prevDiv, 1 };
    int[] xy = euclidInv(b, a % b, a / b);
    a = xy[0];
    xy[0] = a * -prevDiv + xy[1];
    xy[1] = a;
    return xy;
  }
  private static void euclidInv(BigInteger a, BigInteger b,
                                BigInteger prevDiv, BigInteger[] xy)
  {
    if (b.isZero())
      throw new ArithmeticException("not invertible");
    if (b.isOne())
      {
 xy[0] = neg(prevDiv);
        xy[1] = ONE;
 return;
      }
    if (a.words == null)
      {
        int[] xyInt = euclidInv(b.ival, a.ival % b.ival, a.ival / b.ival);
 xy[0] = new BigInteger(xyInt[0]);
        xy[1] = new BigInteger(xyInt[1]);
      }
    else
      {
 BigInteger rem = new BigInteger();
 BigInteger quot = new BigInteger();
 divide(a, b, quot, rem, FLOOR);
        rem.canonicalize();
        quot.canonicalize();
 euclidInv(b, rem, quot, xy);
      }
    BigInteger t = xy[0];
    xy[0] = add(xy[1], times(t, prevDiv), -1);
    xy[1] = t;
  }
  public BigInteger modInverse(BigInteger y)
  {
    if (y.isNegative() || y.isZero())
      throw new ArithmeticException("non-positive modulo");
    if (y.isOne())
      return ZERO;
    if (isOne())
      return ONE;
    BigInteger result = new BigInteger();
    boolean swapped = false;
    if (y.words == null)
      {
        int xval = (words != null || isNegative()) ? mod(y).ival : ival;
        int yval = y.ival;
 if (yval > xval)
   {
     int tmp = xval; xval = yval; yval = tmp;
     swapped = true;
   }
 result.ival =
   euclidInv(yval, xval % yval, xval / yval)[swapped ? 0 : 1];
 if (result.ival < 0)
   result.ival += y.ival;
      }
    else
      {
 BigInteger x = isNegative() ? this.mod(y) : this;
 if (x.compareTo(y) < 0)
   {
     result = x; x = y; y = result;
     swapped = true;
   }
 BigInteger rem = new BigInteger();
 BigInteger quot = new BigInteger();
 divide(x, y, quot, rem, FLOOR);
        rem.canonicalize();
        quot.canonicalize();
 BigInteger[] xy = new BigInteger[2];
 euclidInv(y, rem, quot, xy);
 result = swapped ? xy[0] : xy[1];
 if (result.isNegative())
   result = add(result, swapped ? x : y, 1);
      }
    return result;
  }
  public BigInteger modPow(BigInteger exponent, BigInteger m)
  {
    if (m.isNegative() || m.isZero())
      throw new ArithmeticException("non-positive modulo");
    if (exponent.isNegative())
      return modInverse(m);
    if (exponent.isOne())
      return mod(m);
    BigInteger s = ONE;
    BigInteger t = this;
    BigInteger u = exponent;
    while (!u.isZero())
      {
 if (u.and(ONE).isOne())
   s = times(s, t).mod(m);
 u = u.shiftRight(1);
 t = times(t, t).mod(m);
      }
    return s;
  }
  private static int gcd(int a, int b)
  {
    int tmp;
    if (b > a)
      {
 tmp = a; a = b; b = tmp;
      }
    for(;;)
      {
 if (b == 0)
   return a;
        if (b == 1)
   return b;
        tmp = b;
     b = a % b;
     a = tmp;
   }
      }
  public BigInteger gcd(BigInteger y)
  {
    int xval = ival;
    int yval = y.ival;
    if (words == null)
      {
 if (xval == 0)
   return abs(y);
 if (y.words == null
     && xval != Integer.MIN_VALUE && yval != Integer.MIN_VALUE)
   {
     if (xval < 0)
       xval = -xval;
     if (yval < 0)
       yval = -yval;
     return valueOf(gcd(xval, yval));
   }
 xval = 1;
      }
    if (y.words == null)
      {
 if (yval == 0)
   return abs(this);
 yval = 1;
      }
    int len = (xval > yval ? xval : yval) + 1;
    int[] xwords = new int[len];
    int[] ywords = new int[len];
    getAbsolute(xwords);
    y.getAbsolute(ywords);
    len = MPN.gcd(xwords, ywords, len);
    BigInteger result = new BigInteger(0);
    result.ival = len;
    result.words = xwords;
   if(result.isNegative() && len < xwords.length){
        xwords[len] = 0;
        result.ival++;
    }
    return result.canonicalize();
  }
  public boolean isProbablePrime(int certainty)
  {
    if (certainty < 1)
      return true;
    BigInteger rem = new BigInteger();
    int i;
    for (i = 0; i < primes.length; i++)
      {
 if (words == null && ival == primes[i])
   return true;
        divide(this, smallFixNums[primes[i] - minFixNum], null, rem, TRUNCATE);
        if (rem.canonicalize().isZero())
   return false;
      }
    BigInteger pMinus1 = add(this, -1);
    int b = pMinus1.getLowestSetBit();
    BigInteger m = pMinus1.divide(valueOf(2L << b - 1));
    int bits = this.bitLength();
    for (i = 0; i < k.length; i++)
      if (bits <= k[i])
        break;
    int trials = t[i];
    if (certainty > 80)
      trials *= 2;
    BigInteger z;
    for (int t = 0; t < trials; t++)
      {
 z = smallFixNums[primes[t] - minFixNum].modPow(m, this);
 if (z.isOne() || z.equals(pMinus1))
   continue;
 for (i = 0; i < b; )
   {
     if (z.isOne())
       return false;
     i++;
     if (z.equals(pMinus1))
       break;
     z = z.modPow(valueOf(2), this);
   }
 if (i == b && !z.equals(pMinus1))
   return false;
      }
    return true;
  }
  private void setInvert()
  {
    if (words == null)
      ival = ~ival;
    else
      {
 for (int i = ival; --i >= 0; )
   words[i] = ~words[i];
      }
  }
  private void setShiftLeft(BigInteger x, int count)
  {
    int[] xwords;
    int xlen;
    if (x.words == null)
      {
 if (count < 32)
   {
     set((long) x.ival << count);
     return;
   }
 xwords = new int[1];
 xwords[0] = x.ival;
 xlen = 1;
      }
    else
      {
 xwords = x.words;
 xlen = x.ival;
      }
    int word_count = count >> 5;
    count &= 31;
    int new_len = xlen + word_count;
    if (count == 0)
      {
 realloc(new_len);
 for (int i = xlen; --i >= 0; )
   words[i+word_count] = xwords[i];
      }
    else
      {
 new_len++;
 realloc(new_len);
 int shift_out = MPN.lshift(words, word_count, xwords, xlen, count);
 count = 32 - count;
 words[new_len-1] = (shift_out << count) >> count;
      }
    ival = new_len;
    for (int i = word_count; --i >= 0; )
      words[i] = 0;
  }
  private void setShiftRight(BigInteger x, int count)
  {
    if (x.words == null)
      set(count < 32 ? x.ival >> count : x.ival < 0 ? -1 : 0);
    else if (count == 0)
      set(x);
    else
      {
 boolean neg = x.isNegative();
 int word_count = count >> 5;
 count &= 31;
 int d_len = x.ival - word_count;
 if (d_len <= 0)
   set(neg ? -1 : 0);
 else
   {
     if (words == null || words.length < d_len)
       realloc(d_len);
     MPN.rshift0 (words, x.words, word_count, d_len, count);
     ival = d_len;
     if (neg)
       words[d_len-1] |= -2 << (31 - count);
   }
      }
  }
  private void setShift(BigInteger x, int count)
  {
    if (count > 0)
      setShiftLeft(x, count);
    else
      setShiftRight(x, -count);
  }
  private static BigInteger shift(BigInteger x, int count)
  {
    if (x.words == null)
      {
 if (count <= 0)
   return valueOf(count > -32 ? x.ival >> (-count) : x.ival < 0 ? -1 : 0);
 if (count < 32)
   return valueOf((long) x.ival << count);
      }
    if (count == 0)
      return x;
    BigInteger result = new BigInteger(0);
    result.setShift(x, count);
    return result.canonicalize();
  }
  public BigInteger shiftLeft(int n)
  {
    return shift(this, n);
  }
  public BigInteger shiftRight(int n)
  {
    return shift(this, -n);
  }
  private void format(int radix, StringBuffer buffer)
  {
    if (words == null)
      buffer.append(Integer.toString(ival, radix));
    else if (ival <= 2)
      buffer.append(Long.toString(longValue(), radix));
    else
      {
 boolean neg = isNegative();
 int[] work;
 if (neg || radix != 16)
   {
     work = new int[ival];
     getAbsolute(work);
   }
 else
   work = words;
 int len = ival;
 if (radix == 16)
   {
     if (neg)
       buffer.append('-');
     int buf_start = buffer.length();
     for (int i = len; --i >= 0; )
       {
  int word = work[i];
  for (int j = 8; --j >= 0; )
    {
      int hex_digit = (word >> (4 * j)) & 0xF;
      if (hex_digit > 0 || buffer.length() > buf_start)
        buffer.append(Character_forDigit(hex_digit, 16));
    }
       }
   }
 else
   {
     int i = buffer.length();
     for (;;)
       {
  int digit = MPN.divmod_1(work, work, len, radix);
  buffer.append(Character_forDigit(digit, radix));
  while (len > 0 && work[len-1] == 0) len--;
  if (len == 0)
    break;
       }
     if (neg)
       buffer.append('-');
     int j = buffer.length() - 1;
     while (i < j)
       {
  char tmp = buffer.charAt(i);
  buffer.setCharAt(i, buffer.charAt(j));
  buffer.setCharAt(j, tmp);
  i++; j--;
       }
   }
      }
  }
  public String toString()
  {
    return toString(10);
  }
  public String toString(int radix)
  {
    if (words == null)
      return Integer.toString(ival, radix);
    if (ival <= 2)
      return Long.toString(longValue(), radix);
    int buf_size = ival * (MPN.chars_per_word(radix) + 1);
    StringBuffer buffer = new StringBuffer(buf_size);
    format(radix, buffer);
    return buffer.toString();
  }
  public int intValue()
  {
    if (words == null)
      return ival;
    return words[0];
  }
  public long longValue()
  {
    if (words == null)
      return ival;
    if (ival == 1)
      return words[0];
    return ((long)words[1] << 32) + ((long)words[0] & 0xffffffffL);
  }
  public int hashCode()
  {
    return words == null ? ival : (words[0] + words[ival - 1]);
  }
  private static boolean equals(BigInteger x, BigInteger y)
  {
    if (x.words == null && y.words == null)
      return x.ival == y.ival;
    if (x.words == null || y.words == null || x.ival != y.ival)
      return false;
    for (int i = x.ival; --i >= 0; )
      {
 if (x.words[i] != y.words[i])
   return false;
      }
    return true;
  }
  public boolean equals(Object obj)
  {
    if (! (obj instanceof BigInteger))
      return false;
    return equals(this, (BigInteger) obj);
  }
  private static BigInteger valueOf(String s, int radix)
       throws NumberFormatException
  {
    int len = s.length();
    if (len <= 15 && radix <= 16)
      return valueOf(Long.parseLong(s, radix));
    int byte_len = 0;
    byte[] bytes = new byte[len];
    boolean negative = false;
    for (int i = 0; i < len; i++)
      {
 char ch = s.charAt(i);
 if (ch == '-')
   negative = true;
 else if (ch == '_' || (byte_len == 0 && (ch == ' ' || ch == '\t')))
   continue;
 else
   {
     int digit = Character.digit(ch, radix);
     if (digit < 0)
       break;
     bytes[byte_len++] = (byte) digit;
   }
      }
    return valueOf(bytes, byte_len, negative, radix);
  }
  private static BigInteger valueOf(byte[] digits, int byte_len,
        boolean negative, int radix)
  {
    int chars_per_word = MPN.chars_per_word(radix);
    int[] words = new int[byte_len / chars_per_word + 1];
    int size = MPN.set_str(words, digits, byte_len, radix);
    if (size == 0)
      return ZERO;
    if (words[size-1] < 0)
      words[size++] = 0;
    if (negative)
      negate(words, words, size);
    return make(words, size);
  }
  public double doubleValue()
  {
    if (words == null)
      return (double) ival;
    if (ival <= 2)
      return (double) longValue();
    if (isNegative())
      return neg(this).roundToDouble(0, true, false);
      return roundToDouble(0, false, false);
  }
  public float floatValue()
  {
    return (float) doubleValue();
  }
  private boolean checkBits(int n)
  {
    if (n <= 0)
      return false;
    if (words == null)
      return n > 31 || ((ival & ((1 << n) - 1)) != 0);
    int i;
    for (i = 0; i < (n >> 5) ; i++)
      if (words[i] != 0)
 return true;
    return (n & 31) != 0 && (words[i] & ((1 << (n & 31)) - 1)) != 0;
  }
  private double roundToDouble(int exp, boolean neg, boolean remainder)
  {
    int il = bitLength();
    exp += il - 1;
    if (exp < -1075)
      return neg ? -0.0 : 0.0;
    if (exp > 1023)
      return neg ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
    int ml = (exp >= -1022 ? 53 : 53 + exp + 1022);
    long m;
    int excess_bits = il - (ml + 1);
    if (excess_bits > 0)
      m = ((words == null) ? ival >> excess_bits
    : MPN.rshift_long(words, ival, excess_bits));
    else
      m = longValue() << (- excess_bits);
    if (exp == 1023 && ((m >> 1) == (1L << 53) - 1))
      {
 if (remainder || checkBits(il - ml))
   return neg ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
 else
   return neg ? - Double.MAX_VALUE : Double.MAX_VALUE;
      }
    if ((m & 1) == 1
 && ((m & 2) == 2 || remainder || checkBits(excess_bits)))
      {
 m += 2;
 if ((m & (1L << 54)) != 0)
   {
     exp++;
     m >>= 1;
   }
 else if (ml == 52 && (m & (1L << 53)) != 0)
   exp++;
      }
    m >>= 1;
    long bits_sign = neg ? (1L << 63) : 0;
    exp += 1023;
    long bits_exp = (exp <= 0) ? 0 : ((long)exp) << 52;
    long bits_mant = m & ~(1L << 52);
    return Double.longBitsToDouble(bits_sign | bits_exp | bits_mant);
  }
  private void getAbsolute(int[] words)
  {
    int len;
    if (this.words == null)
      {
 len = 1;
 words[0] = this.ival;
      }
    else
      {
 len = this.ival;
 for (int i = len; --i >= 0; )
   words[i] = this.words[i];
      }
    if (words[len - 1] < 0)
      negate(words, words, len);
    for (int i = words.length; --i > len; )
      words[i] = 0;
  }
  private static boolean negate(int[] dest, int[] src, int len)
  {
    long carry = 1;
    boolean negative = src[len-1] < 0;
    for (int i = 0; i < len; i++)
      {
        carry += ((long) (~src[i]) & 0xffffffffL);
        dest[i] = (int) carry;
        carry >>= 32;
      }
    return (negative && dest[len-1] < 0);
  }
  private void setNegative(BigInteger x)
  {
    int len = x.ival;
    if (x.words == null)
      {
 if (len == Integer.MIN_VALUE)
   set(- (long) len);
 else
   set(-len);
 return;
      }
    realloc(len + 1);
    if (negate(words, x.words, len))
      words[len++] = 0;
    ival = len;
  }
  private void setNegative()
  {
    setNegative(this);
  }
  private static BigInteger abs(BigInteger x)
  {
    return x.isNegative() ? neg(x) : x;
  }
  public BigInteger abs()
  {
    return abs(this);
  }
  private static BigInteger neg(BigInteger x)
  {
    if (x.words == null && x.ival != Integer.MIN_VALUE)
      return valueOf(- x.ival);
    BigInteger result = new BigInteger(0);
    result.setNegative(x);
    return result.canonicalize();
  }
  public BigInteger negate()
  {
    return neg(this);
  }
  public int bitLength()
  {
    if (words == null)
      return MPN.intLength(ival);
      return MPN.intLength(words, ival);
  }
  public byte[] toByteArray()
  {
    byte[] bytes = new byte[(bitLength() + 1 + 7) / 8];
    int nbytes = bytes.length;
    int wptr = 0;
    int word;
    while (nbytes > 4)
      {
 word = words[wptr++];
 for (int i = 4; i > 0; --i, word >>= 8)
          bytes[--nbytes] = (byte) word;
      }
    word = (words == null) ? ival : words[wptr];
    for ( ; nbytes > 0; word >>= 8)
      bytes[--nbytes] = (byte) word;
    return bytes;
  }
  private static int swappedOp(int op)
  {
    return
    "\000\001\004\005\002\003\006\007\010\011\014\015\012\013\016\017"
    .charAt(op);
  }
  private static BigInteger bitOp(int op, BigInteger x, BigInteger y)
  {
    switch (op)
      {
        case 0: return ZERO;
        case 1: return x.and(y);
        case 3: return x;
        case 5: return y;
        case 15: return valueOf(-1);
      }
    BigInteger result = new BigInteger();
    setBitOp(result, op, x, y);
    return result.canonicalize();
  }
  private static void setBitOp(BigInteger result, int op,
          BigInteger x, BigInteger y)
  {
    if (y.words == null) ;
    else if (x.words == null || x.ival < y.ival)
      {
 BigInteger temp = x; x = y; y = temp;
 op = swappedOp(op);
      }
    int xi;
    int yi;
    int xlen, ylen;
    if (y.words == null)
      {
 yi = y.ival;
 ylen = 1;
      }
    else
      {
 yi = y.words[0];
 ylen = y.ival;
      }
    if (x.words == null)
      {
 xi = x.ival;
 xlen = 1;
      }
    else
      {
 xi = x.words[0];
 xlen = x.ival;
      }
    if (xlen > 1)
      result.realloc(xlen);
    int[] w = result.words;
    int i = 0;
    int finish = 0;
    int ni;
    switch (op)
      {
      case 0:
 ni = 0;
 break;
      case 1:
 for (;;)
   {
     ni = xi & yi;
     if (i+1 >= ylen) break;
     w[i++] = ni; xi = x.words[i]; yi = y.words[i];
   }
 if (yi < 0) finish = 1;
 break;
      case 2:
 for (;;)
   {
     ni = xi & ~yi;
     if (i+1 >= ylen) break;
     w[i++] = ni; xi = x.words[i]; yi = y.words[i];
   }
 if (yi >= 0) finish = 1;
 break;
      case 3:
 ni = xi;
 finish = 1;
 break;
      case 4:
 for (;;)
   {
     ni = ~xi & yi;
     if (i+1 >= ylen) break;
     w[i++] = ni; xi = x.words[i]; yi = y.words[i];
   }
 if (yi < 0) finish = 2;
 break;
      case 5:
 for (;;)
   {
     ni = yi;
     if (i+1 >= ylen) break;
     w[i++] = ni; xi = x.words[i]; yi = y.words[i];
   }
 break;
      case 6:
 for (;;)
   {
     ni = xi ^ yi;
     if (i+1 >= ylen) break;
     w[i++] = ni; xi = x.words[i]; yi = y.words[i];
   }
 finish = yi < 0 ? 2 : 1;
 break;
      case 7:
 for (;;)
   {
     ni = xi | yi;
     if (i+1 >= ylen) break;
     w[i++] = ni; xi = x.words[i]; yi = y.words[i];
   }
 if (yi >= 0) finish = 1;
 break;
      case 8:
 for (;;)
   {
     ni = ~(xi | yi);
     if (i+1 >= ylen) break;
     w[i++] = ni; xi = x.words[i]; yi = y.words[i];
   }
 if (yi >= 0) finish = 2;
 break;
      case 9:
 for (;;)
   {
     ni = ~(xi ^ yi);
     if (i+1 >= ylen) break;
     w[i++] = ni; xi = x.words[i]; yi = y.words[i];
   }
 finish = yi >= 0 ? 2 : 1;
 break;
      case 10:
 for (;;)
   {
     ni = ~yi;
     if (i+1 >= ylen) break;
     w[i++] = ni; xi = x.words[i]; yi = y.words[i];
   }
 break;
      case 11:
 for (;;)
   {
     ni = xi | ~yi;
     if (i+1 >= ylen) break;
     w[i++] = ni; xi = x.words[i]; yi = y.words[i];
   }
 if (yi < 0) finish = 1;
 break;
      case 12:
 ni = ~xi;
 finish = 2;
 break;
      case 13:
 for (;;)
   {
     ni = ~xi | yi;
     if (i+1 >= ylen) break;
     w[i++] = ni; xi = x.words[i]; yi = y.words[i];
   }
 if (yi >= 0) finish = 2;
 break;
      case 14:
 for (;;)
   {
     ni = ~(xi & yi);
     if (i+1 >= ylen) break;
     w[i++] = ni; xi = x.words[i]; yi = y.words[i];
   }
 if (yi < 0) finish = 2;
 break;
      default:
      case 15:
 ni = -1;
 break;
      }
    if (i+1 == xlen)
      finish = 0;
    switch (finish)
      {
      case 0:
 if (i == 0 && w == null)
   {
     result.ival = ni;
     return;
   }
 w[i++] = ni;
 break;
      case 1: w[i] = ni; while (++i < xlen) w[i] = x.words[i]; break;
      case 2: w[i] = ni; while (++i < xlen) w[i] = ~x.words[i]; break;
      }
    result.ival = i;
  }
  private static BigInteger and(BigInteger x, int y)
  {
    if (x.words == null)
      return valueOf(x.ival & y);
    if (y >= 0)
      return valueOf(x.words[0] & y);
    int len = x.ival;
    int[] words = new int[len];
    words[0] = x.words[0] & y;
    while (--len > 0)
      words[len] = x.words[len];
    return make(words, x.ival);
  }
  public BigInteger and(BigInteger y)
  {
    if (y.words == null)
      return and(this, y.ival);
    else if (words == null)
      return and(y, ival);
    BigInteger x = this;
    if (ival < y.ival)
      {
        BigInteger temp = this; x = y; y = temp;
      }
    int i;
    int len = y.isNegative() ? x.ival : y.ival;
    int[] words = new int[len];
    for (i = 0; i < y.ival; i++)
      words[i] = x.words[i] & y.words[i];
    for ( ; i < len; i++)
      words[i] = x.words[i];
    return make(words, len);
  }
  public BigInteger or(BigInteger y)
  {
    return bitOp(7, this, y);
  }
  public BigInteger xor(BigInteger y)
  {
    return bitOp(6, this, y);
  }
  public BigInteger not()
  {
    return bitOp(12, this, ZERO);
  }
  public BigInteger andNot(BigInteger val)
  {
    return and(val.not());
  }
  public BigInteger clearBit(int n)
  {
    if (n < 0)
      throw new ArithmeticException();
    return and(ONE.shiftLeft(n).not());
  }
  public BigInteger setBit(int n)
  {
    if (n < 0)
      throw new ArithmeticException();
    return or(ONE.shiftLeft(n));
  }
  public boolean testBit(int n)
  {
    if (n < 0)
      throw new ArithmeticException();
    return !and(ONE.shiftLeft(n)).isZero();
  }
  public BigInteger flipBit(int n)
  {
    if (n < 0)
      throw new ArithmeticException();
    return xor(ONE.shiftLeft(n));
  }
  public int getLowestSetBit()
  {
    if (isZero())
      return -1;
    if (words == null)
      return MPN.findLowestBit(ival);
    else
      return MPN.findLowestBit(words);
  }
  private static final byte[] bit4_count = { 0, 1, 1, 2, 1, 2, 2, 3,
          1, 2, 2, 3, 2, 3, 3, 4};
  private static int bitCount(int i)
  {
    int count = 0;
    while (i != 0)
      {
 count += bit4_count[i & 15];
 i >>>= 4;
      }
    return count;
  }
  private static int bitCount(int[] x, int len)
  {
    int count = 0;
    while (--len >= 0)
      count += bitCount(x[len]);
    return count;
  }
  public int bitCount()
  {
    int i, x_len;
    int[] x_words = words;
    if (x_words == null)
      {
 x_len = 1;
 i = bitCount(ival);
      }
    else
      {
 x_len = ival;
 i = bitCount(x_words, x_len);
      }
    return isNegative() ? x_len * 32 - i : i;
  }
}
