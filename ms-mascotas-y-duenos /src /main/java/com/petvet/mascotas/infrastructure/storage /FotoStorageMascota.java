import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

public class FotoStorageMascota {

    public File seleccionarFoto() {

        JFileChooser selector = new JFileChooser();

        FileNameExtensionFilter filtro =
                new FileNameExtensionFilter(
                        "Imágenes JPG, PNG",
                        "jpg", "jpeg", "png"
                );

        selector.setFileFilter(filtro);

        int resultado = selector.showOpenDialog(null);

        if (resultado == JFileChooser.APPROVE_OPTION) {

            File archivo = selector.getSelectedFile();

            return archivo;
        }

        return null;
    }
}
