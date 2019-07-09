/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package synchronizingdirectories;

import java.io.File;
import java.io.IOException;

/**
 *
 * @author icemen78
 */
public class TmpFile extends File{
    //date 20180824
    private static final long serialVersionUID = -7424875875933669794L;
    private final File origFile;
    private static final String fname_suffix=".tmp";
    private final long lastModified;
    
    public TmpFile(File file) throws IOException {
        super(file.toString()+fname_suffix);
        if (this.exists() && !this.delete()) {throw new IOException ("Не удалось создать временный файл <.tmp> (Файл существует и занят) " + file.toString());}
        origFile=file;
        lastModified=file.lastModified();
    }
    public TmpFile(File file, boolean useCurrentDir) throws IOException {
        super((useCurrentDir?file.getName():file.toString())+fname_suffix);
        if (this.exists() && !this.delete()) {throw new IOException ("Не удалось создать временный файл <.tmp> (Файл существует и занят) " + file.toString());}
        origFile=file;
        lastModified=file.lastModified();
    }
    @Override
    public boolean setLastModified(long time) {
        return super.setLastModified(time) && (super.lastModified()==time);
    }
    public void commit() throws IOException {
        if (origFile.exists()) {
            if (origFile.delete()) {
                if (!this.renameTo(origFile)) {
                    throw new IOException ("Ошибка переименования временного файла ... : " + this.toString());
                }
            }else {
//                rollup();
                throw new IOException ("Ошибка удаления целевого файла ... отмена: " + origFile.toString());
            }
        }else if (!this.renameTo(origFile)){
            throw new IOException ("Ошибка переименования временного файла ... : " + this.toString());
        }
    }
    public void commit(File deleteSourceFile) throws IOException{
        commit();
        if (!origFile.getAbsolutePath().equalsIgnoreCase(deleteSourceFile.getAbsolutePath())) {
            if (!deleteSourceFile.exists()) {
//                throw new IOException ("Исходный файл отсутствует: " + deleteSourceFile.toString());
            }else if (!deleteSourceFile.delete()) {
                throw new IOException ("Ошибка удаления исходного файла. Файл занят: " + deleteSourceFile.toString());
            }
        }
    }
    public void rollup() {
        if (this.exists()) {this.delete();}
    }
}
