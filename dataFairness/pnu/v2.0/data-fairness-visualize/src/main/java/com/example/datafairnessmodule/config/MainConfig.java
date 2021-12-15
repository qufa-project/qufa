package com.example.datafairnessmodule.config;

public class MainConfig {

    public static int ThreadPoolSize = 32;
    public static long AwaitTimeout = 0;
    public static boolean TurnOnConsoleLog = false;
    public static String PrefixSourceTableName = "csvSource";
    public static String PrefixResultTableName = "csvResult";
    public static String MetaTableName = "data_meta";
    public static String StructureTableName = "data_structure";
    public static String StructurePathDelimiter = "|";
    public static String TailDelimiter = "_";
//    public static long RemoveOlder = 1000L * 60 * 60 * 24 * 30; // 지난데이터삭제 30 Days
//    public static long RemoveOlder = 1000L * 60 * 60 * 24; // 지난데이터삭제 24 Hours
    public static long RemoveOlderThen = 1000L * 60 * 10; // 지난데이터삭제 10 Minutes
    public static int EndStdDevNumber = 5; // DB {MetaTableName} col 0 ~ n, stddev 0 ~ n
    public static int LimitRowsTestNonNumeric = 100;
    public static String ImproveLogPrefix(int caseIndex, int i) {
        String p = "[Case " + (caseIndex + 1) + "] ";
        for (int j = 0; j < i; j++) {
            p += "\t";
        }
        return p;
    }
}
