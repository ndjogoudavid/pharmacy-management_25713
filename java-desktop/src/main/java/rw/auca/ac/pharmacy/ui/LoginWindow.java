package rw.auca.ac.pharmacy.ui;

import rw.auca.ac.pharmacy.application.AuthService;
import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public final class LoginWindow extends JFrame {
    private final AuthService auth;
    private final Consumer<String> onSuccess;
    private final JTextField username=new JTextField(20);
    private final JPasswordField password=new JPasswordField(20);

    public LoginWindow(AuthService auth, Consumer<String> onSuccess){
        this.auth=auth;this.onSuccess=onSuccess;setTitle("Pharmacy sign in");setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        JPanel panel=new JPanel(new GridBagLayout());panel.setBorder(BorderFactory.createEmptyBorder(24,24,24,24));
        GridBagConstraints c=new GridBagConstraints();c.insets=new Insets(7,7,7,7);c.anchor=GridBagConstraints.WEST;
        c.gridx=0;c.gridy=0;panel.add(new JLabel("Username"),c);c.gridx=1;panel.add(username,c);
        c.gridx=0;c.gridy=1;panel.add(new JLabel("Password"),c);c.gridx=1;panel.add(password,c);
        JButton login=new JButton("Sign in");c.gridx=1;c.gridy=2;c.anchor=GridBagConstraints.EAST;panel.add(login,c);login.addActionListener(e->login());password.addActionListener(e->login());
        add(panel);getRootPane().setDefaultButton(login);pack();setResizable(false);setLocationRelativeTo(null);setVisible(true);
    }
    private void login(){char[] secret=password.getPassword();try{if(auth.authenticate(username.getText(),secret)){String name=username.getText().trim();dispose();onSuccess.accept(name);}else{JOptionPane.showMessageDialog(this,"Username or password is incorrect.","Sign in",JOptionPane.WARNING_MESSAGE);password.setText("");}}catch(RuntimeException ex){UiSupport.error(this,ex);}finally{java.util.Arrays.fill(secret,'\0');}}
}
