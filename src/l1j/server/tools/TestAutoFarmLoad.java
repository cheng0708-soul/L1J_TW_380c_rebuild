package l1j.server.tools;

public class TestAutoFarmLoad {
    public static void main(String[] args) {
        String cls = "l1j.server.server.model.item.function.AutoFarmToggle";
        System.out.println("[TEST] CP hint: ensure l1jserver.jar or bin/classes on classpath");
        try {
            System.out.println("[TEST] Trying to load: " + cls);
            Class<?> c = Class.forName(cls);
            System.out.println("[TEST] Class.forName OK: " + c.getName());
            try {
                java.lang.reflect.Method m = c.getMethod("get");
                System.out.println("[TEST] get() method OK: " + m);
                Object exec = m.invoke(null);
                System.out.println("[TEST] get() invoke OK: instance=" + exec);
            } catch (Throwable t) {
                System.out.println("[TEST] ERROR invoking get(): " + t);
                t.printStackTrace(System.out);
            }
        } catch (Throwable e) {
            System.out.println("[TEST] ERROR Class.forName: " + e);
            e.printStackTrace(System.out);
        }
    }
}