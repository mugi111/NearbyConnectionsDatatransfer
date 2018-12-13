package com.mugi111.nearbyconnectionsdatatransfer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Zipファイルのユーティリティー
 */
public class ZipFileUtils {

    /**
     * フォルダをzipファイルに圧縮
     * @param input 圧縮するフォルダ
     * @param output zipファイル出力先
     * @param fileName zipファイル名
     * @return 圧縮できたか
     */
    public static boolean compressFolder(File input, File output, String fileName) {
        try {
            ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(output + "/" + fileName)));
            archive(zipOutputStream, input, output);

            zipOutputStream.closeEntry();
            zipOutputStream.flush();
            zipOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * フォルダ圧縮
     * @param zipOutputStream ZipOutputStream
     * @param input 圧縮するフォルダ
     * @param output zipファイル出力先
     */
    private static void archive(ZipOutputStream zipOutputStream, File input, File output) {
        if (input.isDirectory()) {
            File[] files = input.listFiles();

            for (File file : files) {
                if (file.isDirectory()) {
                    archive(zipOutputStream, file, output);
                } else {
                    if (!file.getAbsoluteFile().equals(output)) {
                        archive(zipOutputStream, file, file.getPath().replace(SDCardUtils.getFolderPath().getPath(), "").substring(1));
                    }
                }
            }
        }
    }

    /**
     * ファイル圧縮
     * @param zipOutputStream ZipOutputStream
     * @param input 圧縮するファイル
     * @param entry zip内のパス
     * @return 圧縮できたか
     */
    private static boolean archive(ZipOutputStream zipOutputStream, File input, String entry) {
        // zipの圧縮レベル1～9　高レベルほど高圧縮だが時間がかかる
        zipOutputStream.setLevel(6);

        try {
            // zipエントリ作成
            zipOutputStream.putNextEntry(new ZipEntry(entry));
            BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(input));

            // 圧縮ファイルをzipファイルに出力
            int len;
            byte[] buf = new byte[1024];
            while ((len = inputStream.read(buf, 0, buf.length)) != -1) {
                zipOutputStream.write(buf, 0, len);
            }

            inputStream.close();
            zipOutputStream.closeEntry();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * zipファイルの解凍
     * @param zipPath zipファイルのパス
     * @param unZipPath zipファイルの解凍先パス
     * @return 解凍できたか
     */
    public static boolean unZip(File zipPath, File unZipPath) {
        try {
            ZipFile zipFile = new ZipFile(zipPath);

            // zipファイル内のファイルを取得
            Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();

            // zipファイル内のファイルを展開
            while (zipEntries.hasMoreElements()) {
                ZipEntry zipEntry = zipEntries.nextElement();

                // 出力先
                File output = new File(unZipPath.getPath(), zipEntry.getName());

                if (zipEntry.isDirectory()) {
                    output.mkdirs();
                } else {
                    BufferedInputStream inputStream = new BufferedInputStream(zipFile.getInputStream(zipEntry));

                    if (!output.getParentFile().exists()) {
                        output.getParentFile().mkdirs();
                    }

                    BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(output));

                    int len;
                    byte[] buf = new byte[1024];
                    while ((len = inputStream.read(buf)) != -1) {
                        outputStream.write(buf, 0, len);
                    }

                    inputStream.close();
                    outputStream.close();
                }
            }
            zipFile.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
