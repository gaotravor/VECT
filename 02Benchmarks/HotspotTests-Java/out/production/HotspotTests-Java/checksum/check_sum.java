//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package checksum;

import com.alibaba.fastjson.JSONArray;
import java.util.Locale;

public class check_sum {
    public check_sum() {
    }

    public static int run(int lastSum, Object o) {
        if (o instanceof String) {
            String[] filters = new String[]{"time", "exception", "error", "failure", "jdk", "jre", "system"};
            String[] var3 = filters;
            int var4 = filters.length;

            for(int var5 = 0; var5 < var4; ++var5) {
                String filter = var3[var5];
                if (((String)o).toLowerCase(Locale.ROOT).contains(filter)) {
                    return 0;
                }
            }
        }

        if (!(o instanceof Runnable) && !(o instanceof Exception)) {
            try {
                return Integer.hashCode(Arrays.toString(JSONArray.toJSONBytes(checksumHelper, SerializerFeature.DisableCircularReferenceDetect)).hashCode());
            } catch (Exception var7) {
                return 0;
            }
        } else {
            return 0;
        }
    }

    public static int run(int lastSum, byte b) {
        lastSum += Byte.hashCode(b);
        return Integer.hashCode(lastSum);
    }

    public static int run(int lastSum, short s) {
        lastSum += Short.hashCode(s);
        return Integer.hashCode(lastSum);
    }

    public static int run(int lastSum, int i) {
        lastSum += Integer.hashCode(i);
        return Integer.hashCode(lastSum);
    }

    public static int run(int lastSum, long l) {
        lastSum += Long.hashCode(l);
        return Integer.hashCode(lastSum);
    }

    public static int run(int lastSum, float f) {
        lastSum += Float.hashCode(f);
        return Integer.hashCode(lastSum);
    }

    public static int run(int lastSum, double d) {
        lastSum += Double.hashCode(d);
        return Integer.hashCode(lastSum);
    }

    public static int run(int lastSum, char c) {
        lastSum += Character.hashCode(c);
        return Integer.hashCode(lastSum);
    }

    public static int run(int lastSum, boolean b) {
        lastSum += Boolean.hashCode(b);
        return Integer.hashCode(lastSum);
    }
}
