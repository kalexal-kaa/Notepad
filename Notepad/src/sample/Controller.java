package sample;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;

public class Controller implements Initializable {
    @FXML
    private TextArea t;
    @FXML
    private ListView<String> list;
    @FXML
    private Label userLogin;

    private String passwordKey;
    private String userDirectoryPath;
    private final String separator;
    private final Toast toast;
    private final Cryptograph cryptograph;

    public Controller() {
        this.toast = new Toast();
        this.cryptograph = new Cryptograph();
        this.separator = System.getProperty("file.separator");
    }
    @FXML
    private void createNoteAction() throws IOException {
        final TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Создание записи");
        dialog.setHeaderText("Введите название новой записи");
        dialog.setContentText("Название записи:");
        final Optional<String> name = dialog.showAndWait();
        if (name.isPresent() && !isEmpty(name.get())) {
            if (incorrectSymbols(name.get())){
                alertWindow("Запись не будет создана:\nОбнаружены недопустимые символы в названии записи");
                createNoteAction();
                return;
            }
            File file = new File(userDirectoryPath+separator+name.get());
            if (file.exists()) {
                alertWindow("Запись не будет создана:\nЗапись с таким названием уже имеется");
                createNoteAction();
                return;
            }
            if (file.createNewFile()){
                showNotes(file.getParent());
                list.getSelectionModel().select(file.getName());
                t.clear();
            }
        }
    }
    @FXML
    private void saveNoteAction(){
        String l=list.getSelectionModel().getSelectedItem();
        if(l==null){
            alertWindow("Выберите запись из списка слева.\nЕсли список пуст, создайте новую запись.");
            if(!isEmpty(t.getText())){
                StringSelection ss = new StringSelection(t.getText());
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
                toast.setMessage("Сохранено в буфер обмена");
            }
            return;
        }
        if(isEmpty(t.getText())){
            toast.setMessage("Ничего нет для сохранения");
            return;
        }
        writer(userDirectoryPath + separator + l , cryptograph.encode(t.getText(),passwordKey));
        toast.setMessage("Сохранение");
    }
    @FXML
    private void deleteNoteAction() throws IOException {
        File f=new File(userDirectoryPath+separator+list.getSelectionModel().getSelectedItem());
        if(f.delete()){
            showNotes(userDirectoryPath);
            t.clear();
        }else{
            toast.setMessage("Ошибка удаления");
        }
    }
    @FXML
    private void listItemAction(){
        t.setText(cryptograph.decode(reader(userDirectoryPath+separator+this.list.getSelectionModel().getSelectedItem()),passwordKey));
    }
    public void exit(){
        final Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setResizable(true);
        alert.getDialogPane().setPrefSize(500,140);
        alert.setTitle("ВЫХОД");
        alert.setHeaderText("Выйти из программы?");
        alert.setContentText("Несохраненные изменения будут потеряны навсегда.");
        final Optional<ButtonType> resultAlert = alert.showAndWait();
        if (resultAlert.get() == ButtonType.OK) {
            System.exit(0);
        }
    }
    private void alertWindow(final String s) {
        final Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setResizable(true);
        alert.getDialogPane().setPrefSize(400, 140);
        alert.setTitle("Внимание!");
        alert.setHeaderText("");
        alert.setContentText(s);
        alert.showAndWait();
    }
    private boolean isEmpty(String s){
        return s == null || s.trim().length() == 0;
    }
    private boolean incorrectSymbols(String str){
            return str.contains(":")||str.contains("*")||str.contains("/")||str.contains("|")
                    ||str.contains("?")||str.contains("#")||str.contains("!")||str.contains("$")||str.contains("%")||str.contains(";");
    }
    private void showNotes(String path){
        File files=new File(path);
        File[] f=files.listFiles();
        assert f != null;
        String[] notes=new String[f.length];
        for(int i=0;i<notes.length;i++){
            notes[i]=f[i].getName();
        }
        list.setItems(FXCollections.observableArrayList(notes).sorted());
    }
    private String reader(final String s) {
        StringBuilder f=new StringBuilder();
        try {
            final File file = new File(s);
            final BufferedReader br;
            try (FileReader fr = new FileReader(file)) {
                br = new BufferedReader(fr);
                String str;
                while ((str = br.readLine()) != null) {
                    f.append(str);
                }
            }
            br.close();
        }
        catch (IOException e) {
            e.getMessage();
        }
        return f.toString();
    }
    private void dirCreator(final String fPath) {
        final File file = new File(fPath);
        if (!file.exists()) {
            file.mkdir();
            if(!file.exists()){
                alertWindow("Ошибка!\nКаталог <Notepad> не будет создан, а программа будет закрыта."
                                + "\nПопробуйте создать указанный каталог вручную по следующему пути:\n"+fPath);
                System.exit(0);
            }
        }
    }
    private boolean permissionRead(File file){
        if(!file.canRead()){
            file.setReadable(true);
            return !file.canRead();
        }
        return false;
    }
    private boolean permissionWrite(File file){
        if(!file.canWrite()){
            file.setWritable(true);
            return !file.canWrite();
        }
        return false;
    }
    private void writer(String pathFile,String text){
        try (final FileWriter fw = new FileWriter(pathFile)) {
            fw.write(text);
        } catch (IOException e) {
            e.getMessage();
        }
    }
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        String key = " ";
        String parentPath=null;
        try {
            parentPath=URLDecoder.decode(new File(getClass().getProtectionDomain().getCodeSource().getLocation().getPath()).getParent(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.getMessage();
        }
        String path=parentPath+separator+"Notepad";
        this.dirCreator(path);
        File f=new File(path);
        if(permissionRead(f)||permissionWrite(f)){
            if(permissionRead(f)&&permissionWrite(f)){
                alertWindow("Не удалось получить разрешение на чтение и запись файлов в каталог <Notepad>.\nПопробуйте дать разрешение вручную.");
            }else if(permissionRead(f)){
                alertWindow("Не удалось получить разрешение на чтение файлов в каталоге <Notepad>.\nПопробуйте дать разрешение вручную.");
            }else{
                alertWindow("Не удалось получить разрешение на запись файлов в каталог <Notepad>.\nПопробуйте дать разрешение вручную.");
            }
            System.exit(0);
        }
        Dialog dialog = new Dialog<>();
        dialog.setTitle("Вход в блокнот");
        dialog.setHeaderText("Введите логин и пароль");

        ButtonType loginButtonType = new ButtonType("Войти", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType  = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType registrationButtonType = new ButtonType("Регистрация",ButtonBar.ButtonData.FINISH);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType,cancelButtonType,registrationButtonType);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField login = new TextField();
        TextField password = new PasswordField();
       
        grid.add(new Label("Логин:"), 0, 0);
        grid.add(login, 1, 0);
        grid.add(new Label("Пароль:"), 0, 1);
        grid.add(password, 1, 1);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if(result.get()==registrationButtonType){
            Dialog d = new Dialog<>();
            d.setTitle("Регистрация");
            d.setHeaderText("Придумайте логин и пароль");

            ButtonType doneButtonType = new ButtonType("Готово", ButtonBar.ButtonData.OK_DONE);
            ButtonType cButtonType  = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
            d.getDialogPane().getButtonTypes().addAll(doneButtonType,cButtonType);

            GridPane g = new GridPane();
            g.setHgap(10);
            g.setVgap(10);
            g.setPadding(new Insets(20, 150, 10, 10));

            TextField loginField = new TextField();
            TextField passwordField = new TextField();
       
            g.add(new Label("Логин:"), 0, 0);
            g.add(loginField, 1, 0);
            g.add(new Label("Пароль:"), 0, 1);
            g.add(passwordField, 1, 1);

            d.getDialogPane().setContent(g);

            Optional<ButtonType> resulOptional = d.showAndWait();
            
        if(resulOptional.get()==doneButtonType){
            
            String loginString=loginField.getText().trim();
            String passwordString=passwordField.getText().trim();
            
            if (isEmpty(loginString)){
                alertWindow("Введите логин");
                initialize(location, resources);
            }
            if (isEmpty(passwordString)){
                alertWindow("Введите пароль");
                initialize(location, resources);
            }
            File file = new File(path+separator+loginString+separator+"Notes");
            if(file.exists()){
                alertWindow("Пользователь с логином "+loginString+" уже существует");
                initialize(location, resources);
            }else{
                if(file.mkdirs()){
                    writer(path+separator+loginString+separator+"p", cryptograph.encode(passwordString, key));
                    initialize(location, resources);
                }
            }
        }else{
            initialize(location, resources);
        }
        }
        if (result.get()==loginButtonType){
            
            String lString=login.getText().trim();
            String pString=password.getText().trim();
            
            if (isEmpty(lString)){
                alertWindow("Введите логин");
                initialize(location, resources);
            }
            if (isEmpty(pString)){
                alertWindow("Введите пароль");
                initialize(location, resources);
            }
            File file = new File(path+separator+lString);
            if(file.exists()){
                passwordKey = cryptograph.decode(reader(path+separator+lString+separator+"p"), key);
                if(pString.equals(passwordKey)){
                    userDirectoryPath=path+separator+lString+separator+"Notes";
                    userLogin.setText(lString);
                    showNotes(userDirectoryPath);
                }else{
                    alertWindow("Неверный пароль");
                    initialize(location, resources);
                }
            }else{
                alertWindow("Пользователь с логином "+lString+" не существует");
                initialize(location, resources);
            }
        }
        if(result.get()==cancelButtonType){
            System.exit(0);
        }
    }
}
