
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Ron
 */
public class TestingSandbox {

    public void runTest() {
        
        boolean forceGC = false;
        
        getMemoryUse(forceGC);
        
        System.out.print("test starting\n");
        getMemoryUse(forceGC);

        TestObjectOuterWrapper toow = new TestObjectOuterWrapper();

        System.out.print("created TestObjectWrapper\n");
        getMemoryUse(forceGC);

        TestObjectDao dao = new TestObjectDao();

        System.out.print("created DAO\n");
        getMemoryUse(forceGC);

        for (int y = 0; y < 500000; y++) {
            Set<TestObject> set = new HashSet<>(1000,500);
            for (int i = 0; i < 5000; i++) {
                //System.out.print("grabbing test object "+i+"\n");
                set.add(dao.get(i + ""));
            }
            System.out.print("making test object holder " + y + "\n");
            TestObjectHolder toh = new TestObjectHolder(y, set);

            getMemoryUse(forceGC);
        }
        System.out.print("test object holders count=" + toow.allHolders.size() + "\n");
    }

    public class TestObjectOuterWrapper implements Serializable {

        String toowID = "TOOWid";
        public ArrayList<TestObjectHolder> allHolders = new ArrayList<>();
    }

    /**
     *
     */
    public class TestObjectHolder implements Serializable {

        String someOtherString1;
        String someOtherString2;
        String someOtherString3;
        String someOtherString4;
        HashSet<TestObject> testObjectSet = new HashSet<>(1000,500);

        public TestObjectHolder(int someThingUnique, Set<TestObject> toSet) {
            this.someOtherString1 = "SOMTHING1" + someThingUnique;
            this.someOtherString2 = "SOMTHING2" + someThingUnique;
            this.someOtherString3 = "SOMTHING3" + someThingUnique;
            this.someOtherString4 = "SOMTHING4" + someThingUnique;

            this.testObjectSet.addAll(toSet);
        }

    }

    /**
     *
     */
    public class TestObjectDao {

        public Map<String, TestObject> allObjects = new HashMap(1000,500);

        public TestObjectDao() {
            int cnt = 10000;
            System.out.print("genratating " + cnt + " object\n");
            for (int i = 0; i < cnt; i++) {
                TestObject to = new TestObject(i);
                allObjects.put(to.getKey(), to);
            }
        }

        public TestObject get(String key) {
            return allObjects.get(key);
        }
    }

    /**
     *
     */
    public class TestObject implements Serializable {

        int key = 123435325;
        String str2[] = new String[]{"xxxxx", "yyyyyy", "zzzzzz"};
        String str3 = new String("THIS IS A TEST STRING");

        TestObject(int key) {
            this.key = key;
            this.str3 = str3 + key;
        }

        String getKey() {
            return key + "";
        }

        @Override
        public int hashCode() {
            return key;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final TestObject other = (TestObject) obj;
            if (this.key != other.key) {
                return false;
            }
            return true;
        }

    }

    /**
     *
     * 
     * 
     * @return
     */
    private long getMemoryUse(boolean forceGC ) {
        
        if(forceGC){
            forceGarbageCollection();
            forceGarbageCollection();
        }

        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();

        long usedMemory = (totalMemory - freeMemory);
        
        System.out.print("memory used up =" + usedMemory + "/"+totalMemory+" ("+(usedMemory*100/totalMemory)+"%)\n");

        return (totalMemory - freeMemory);
    }
    
    /**
     * 
     * 
     * 
     */
    private void forceGarbageCollection() {
        try {
            System.gc();
            Thread.currentThread().sleep(100);
            System.runFinalization();
            Thread.currentThread().sleep(100);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    /**
     *
     * @param any
     * @throws IOException
     */
    public static void dumpByteSize(Serializable any) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        byte[] objBytes = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(any);
            out.flush();
            objBytes = bos.toByteArray();
            System.out.print("total size=" + objBytes.length + "\n");
        } catch (IOException ex) {
            System.out.print("ERROR:" + ex + "\n");
        } finally {
            try {
                bos.close();
            } catch (IOException ex) {
                // ignore close exception
            }
        }

    }

    public static void main(String[] args) throws Exception {
        System.out.print("....start\n");

        TestingSandbox sb = new TestingSandbox();
        sb.runTest();

        System.out.print("....done\n");

    }

}
