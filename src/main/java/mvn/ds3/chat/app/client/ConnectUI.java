package mvn.ds3.chat.app.client;

import mvn.ds3.chat.app.shared.msg.MsgChat;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class ConnectUI extends JFrame {

    private ControllerApp controller = null;
    private JButton joinBtn = new JButton("Join");
    private JTextArea nameChat = new JTextArea(1, 10);
    private JLabel userLbl = new JLabel("User");
    JPanel chatJDialogue = new JPanel();

    public ConnectUI(ControllerApp controller) {
        this.controller = controller;

        nameChat.setLineWrap (true);
        nameChat.setWrapStyleWord (true); //default
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Chat Client Application");
        setSize(300, 200);

        chatJDialogue.setLayout(new GridLayout(2, 2));

        add(userLbl);
        var downPnl = new JPanel();
        add(downPnl);
        joinBtn.setSize(100, 50);
        downPnl.add(userLbl);
        downPnl.add(nameChat);
        downPnl.add(joinBtn);

        chatJDialogue.setVisible(true);

        // starte in der mitte
        Dimension dmsn = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation(dmsn.width/2-this.getSize().width/2, dmsn.height/2-this.getSize().height/2);

        // reagiere auf key + button events
        registerKey(nameChat);
        registerHandlers();

    }

    private void registerHandlers() {

        joinBtn.addActionListener(l -> {
        var msgUI = new MsgUI(controller, nameChat.getText() );
        msgUI.setVisible(true);
        setVisible(false);
        });

    }

    private void registerKey(JTextArea nameChat) {
        nameChat.addKeyListener(new KeyListener(){
            @Override
            public void keyPressed(KeyEvent key){
                if(key.getKeyCode() == KeyEvent.VK_ENTER){
                    var msgUI = new MsgUI(controller, nameChat.getText() );
                    System.out.println(nameChat.getText() + " has joined chat");
                    msgUI.setVisible(true);
                    setVisible(false);
                }
            }

            @Override
            public void keyTyped(KeyEvent key) {
            }

            @Override
            public void keyReleased(KeyEvent key) {
            }
        });

    }

}
