package com.mugi111.nearbyconnectionsdatatransfer;

import android.os.Environment;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * SDカードの読み書きユーティリティー<br>
 * ファイル関連のユーティリティーにもなってきた
 */
public class SDCardUtils {
    /** このアプリのファイル書き出し先の親フォルダ */
    public static final String APPLICATION_FOLDER = "HopStepMap";

    /** ファイル数カウント用 */
    private static int sFileCount;

    /**
     * 外付けSDカードのパス取得
     * @return 外付けSDカードのパス
     */
    public static File getExternalStorageFolder() {
        List<String> mountList = new ArrayList<>();

        Scanner scanner = null;
        try {
            File vold_fstab = new File("/system/etc/vold.fstab");
            scanner = new Scanner(new FileInputStream(vold_fstab));
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.startsWith("dev_mount") || line.startsWith("fuse_mount")) {
                    String path = line.replaceAll("\t", " ").split(" ")[2];
                    if (!mountList.contains(path)){
                        mountList.add(path);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }

        if (!Environment.isExternalStorageRemovable()) {
            mountList.remove(Environment.getExternalStorageDirectory().getPath());
        }

        for (int i = 0; i < mountList.size(); i++) {
            if (!isMounted(mountList.get(i))){
                mountList.remove(i--);
            }
        }

        if(mountList.size() > 0){
            return new File(mountList.get(0));
        }

        return null;
    }

    /**
     * マウントされているか
     * @param path パス
     * @return true:マウントされている false:マウントされていない
     */
    public static boolean isMounted(String path) {
        boolean isMounted = false;

        Scanner scanner = null;
        try {
            File mounts = new File("/proc/mounts");
            scanner = new Scanner(new FileInputStream(mounts));
            while (scanner.hasNextLine()) {
                if (scanner.nextLine().contains(path)) {
                    isMounted = true;
                    break;
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }

        return isMounted;
    }

    /**
     * SDカードのパス取得
     * @return パス
     */
    public static File getFolderPath() {
        File path;

        if (hasSDCard()) {
            path = new File(getSDCardPath());
        } else {
            path = Environment.getDataDirectory();
        }

        return path;
    }

    /**
     * 指定フォルダのパス取得<br>
     * フォルダが存在しなかったらフォルダ作成
     * @param dir 指定フォルダ
     * @return 指定フォルダのパス
     */
    public static File getFolderPath(String dir) {
        File path = new File(getFolderPath().getPath() + "/" + dir);

        path.mkdirs();

        return path;
    }

    /**
     * 指定ファイルパス取得
     * @param folder ファイルが入ってるフォルダ(SD直下フォルダではない場合それまでの階層も指定)<br>
     * 例:)HSMの1班のアイコン情報取得したい場合、"HopStepMap/1班"を第1引数に
     * @param fileName ファイル名
     * @return 指定ファイルパス
     */
    public static File getFilePath(String folder, String fileName) {
        return new File(getFolderPath(folder).getPath() + "/" + fileName);
    }

    /**
     * ファイルまたはフォルダを削除<br>
     * フォルダの中にファイルがある場合中身全削除
     * @param file ファイル or フォルダ
     * @return 削除できたか
     */
    public static boolean delete(File file) {
        if (!file.exists()) {
            return false;
        }

        if (file.isFile()) {
            return file.delete();
        } else if (file.isDirectory()) {
            File[] files = file.listFiles();

            // フォルダ内のファイルを再帰的に削除
            for (File file1 : files) {
                delete(file1);
            }

            // フォルダ削除
            return file.delete();
        }

        return true;
    }

    /**
     * フォルダをコピー＆移動
     * @param from コピー元のフォルダ
     * @param to コピー先のフォルダ
     */
    public static void copyTransfer(File from, File to) {
        if (from.isDirectory()) {
            // コピー先のフォルダが作られていなかったら作成
            if (!to.exists()) {
                to.mkdirs();
            }

            String[] files = from.list();

            for (String file : files) {
                copyTransfer(new File(from, file), new File(to, file));
            }
        } else {
            // コピー先のファイルの親フォルダが作られてなかったら作成
            if (!to.getParentFile().exists()) {
                to.getParentFile().mkdirs();
            }
            try {
                FileChannel fromChannel = new FileInputStream(from).getChannel();
                FileChannel toChannel = new FileOutputStream(to).getChannel();

                fromChannel.transferTo(0, fromChannel.size(), toChannel);

                fromChannel.close();
                toChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * ファイルのコピー
     * @param inputStream InputStream
     * @param outputStream OutputStream
     * @return コピーできたか
     */
    public static boolean copyFile(BufferedInputStream inputStream, BufferedOutputStream outputStream) {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                outputStream.write(buf, 0, len);
                outputStream.flush();
            }

            inputStream.close();
            outputStream.close();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    /**
     * 指定フォルダ以下にファイルが存在するか
     * @param file 指定フォルダ
     * @return ファイルが存在するか
     */
    public static boolean hasFile(File file) {
        sFileCount = 0;

        countFile(file);

        return sFileCount > 0;
    }

    /**
     * 指定フォルダ以下のファイル数カウント
     * @param file 指定フォルダ
     */
    public static void countFile(File file) {
        if (file.isFile()) {
            sFileCount++;
        } else if (file.isDirectory()) {
            File[] files = file.listFiles();

            // フォルダ内にファイルが存在するか再帰的にチェック
            for (File file1 : files) {
                hasFile(file1);
            }
        }
    }

    /**
     * 指定フォルダ以下の空のフォルダ削除
     * @param file 指定フォルダ
     */
    public static void deleteEmptyFolder(File file) {
        File[] files = file.listFiles();

        if (files == null) {
            return;
        } else if (files.length == 0) {
            file.delete();
            return;
        }

        for (File file1 : files) {
            if (file1.isDirectory()) {
                deleteEmptyFolder(file1);
            }
        }
    }

    /**
     * フォルダサイズ取得
     * @param directory 取得するフォルダ
     * @return フォルダサイズ
     */
    public static long getFolderSize(File directory) {
        long length = 0;
        for (File file : directory.listFiles()) {
            if (file.isFile()) {
                length += file.length();
            } else {
                length += getFolderSize(file);
            }
        }
        return length;
    }

    /**
     * SDカードの空き容量取得
     * @return SDカードの空き容量
     */
    public static long getFreeSpace() {
        return getFolderPath().getFreeSpace();
    }


    /**
     * SDカードがあるか
     * @return true:ある false:ない
     */
    public static boolean hasSDCard() {
        String status = Environment.getExternalStorageState();
        return status.equals(Environment.MEDIA_MOUNTED);
    }

    /**
     * SDカードのパス取得
     * @return SDカードのパス
     */
    public static String getSDCardPath() {
        File path = Environment.getExternalStorageDirectory();
        return path.getAbsolutePath();
    }

    /**
     * SDカードに書き込み
     * @param path 保存先のPath
     * @param text 保存内容
     */
    public static void write(String path, String text) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(path));
            writer.write(text);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
