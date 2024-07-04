package test.unit;

import v2.TorrentInfo;

public class TestTorrentInfo {
        public static void main(String[] args) {
               System.out.println("TestTorrentInfo");

               System.out.println(TorrentInfo.fromFile("test/unit/mytest.torrent").toFullString());
        }
}
