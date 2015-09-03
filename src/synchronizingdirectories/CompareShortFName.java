/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/*
    Компаратор. Предписывает осуществлять сравнение файлов по короткому имени 
*/

package synchronizingdirectories;

import java.io.File;
import java.util.Comparator;

/**
 *
 * @author icemen78
 */
public class CompareShortFName implements Comparator {
    @Override
    public int compare(Object t, Object t1) {
        String st, st1;
        int it, it1;
        st = t.toString();
        st1 = t1.toString();
        if (t.getClass().equals(File.class) & t1.getClass().equals(File.class)) {
            it = (st.lastIndexOf(File.separator));
            it1 = st1.lastIndexOf(File.separator);
            it = (it==-1?0:it);
            it1 = (it1==-1?0:it1);
            return st.substring(it).compareTo(st1.substring(it1));            
        } else {
            return t.toString().compareTo(t1.toString());
        }
    }
}
