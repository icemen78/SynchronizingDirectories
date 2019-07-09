/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/*
    Класс инкапсулирующий пару объектов: ошибку, произошедшую при попытке синхронизации
    объекта FileEnrty и соответствующее имя файла.
*/
package synchronizingdirectories;


import java.lang.reflect.Field;
import java.util.TreeMap;

/**
 *
 * @author icemen78
 */
public class ErrorEntry implements Comparable{
    TreeMap<String, Exception> errfiles = new TreeMap<>();
    FileEntry fileentry;

    public ErrorEntry(FileEntry fileentry, TreeMap<String,Exception> errfiles) {
        this.fileentry = fileentry;
        this.errfiles.putAll(errfiles);
    }
    public TreeMap<String, Exception> getDetails() {
        return errfiles;
    }
    public String getState() {
        //Object states[];
        String retval = "Unknown";
        Field[] myEnums = fileentry.getClass().getFields();
        for (Field fl:myEnums){
            if (fl.isEnumConstant()) {
//                System.out.println(fl.);
            }
        }
        //Нужно реализовать функцию вывода имени константы
//////        switch (state) {
//////            case FileEntry.FE_MAST_NEW:
//////               //states=FileEntry.class.getEnumConstants();
//////                retval = "New";
//////                break;
//////            case FileEntry.FE_MAST_REPLACE:
//////                retval = "Replace";
//////                break;
//////            case FileEntry.FE_MAST_DELETE:
//////                retval = "Delete";
//////                break;
//////            case FileEntry.FE_FILE_AND_FOLDER:
//////                retval = "FileAndFolder";
//////                break;
//////        }
        return retval;
    }
    public int getStateNumber() {
        return fileentry.getState();
    }
    public FileEntry getFileEntry(){
        return fileentry;
    }
    @Override
    public String toString() {
        return fileentry.toString();
    }
    @Override
    public int compareTo(Object t) {
        return this.toString().compareTo(t.toString());
    }
}
