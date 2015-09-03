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

/**
 *
 * @author icemen78
 */
public class ErrorEntry {
    private final Exception error;
    private final String filename;
    private final int state;
    public ErrorEntry(Exception ex, String fname, int st) {
        error = ex;
        filename = fname;
        state = st;
    }
    public String getFilename() {
        return filename;
    }
    public Exception getError() {
        return error;
    }
    public String getState() {
        String retval = "Unknown";
        switch (state) {
            case FileEntry.FE_MAST_NEW: 
                retval = "New";
                break;
            case FileEntry.FE_MAST_REPLACE:
                retval = "Replace";
                break;
            case FileEntry.FE_MAST_DELETE:
                retval = "Delete";
                break;
        }
        return retval;
    }
    public int getStateNumber() {
        return state;
    }
}
