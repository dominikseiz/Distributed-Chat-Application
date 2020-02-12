package mvn.ds3.chat.app.client;

import mvn.ds3.chat.app.shared.msg.MsgChat;

import javax.swing.*;
import java.awt.*;

public class MsgUI extends JFrame {

    private final ControllerApp controller;
    private String nameApp;

    private JTextArea chatArea = new JTextArea(30, 30);
    private JButton sendBtn = new JButton("Send");
    private JTextArea sendArea = new JTextArea(5, 30);
    private JLabel userLbl = new JLabel("User");

    public MsgUI(ControllerApp controller, String nameApp) {
        this.controller = controller;
        this.nameApp = nameApp;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setTitle("App Chatroom");
        setSize(450, 300);


        setLayout(new GridLayout(2, 2));
        // upper chat part
        chatArea.setEditable(false);
        add(chatArea);
        JScrollPane chatAreaScrllPn = new JScrollPane(chatArea,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        add(chatAreaScrllPn);

        var downPnl = new JPanel();
        sendBtn.setSize(100, 50);
        userLbl.setText(nameApp);
        downPnl.add(userLbl);
        downPnl.add(sendArea);
        downPnl.add(sendBtn);

        add(downPnl);

        // Erzeugung eines JSplitPane-Objektes mit horizontaler Trennung
        JSplitPane spltPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        // Hier setzen wir links unser rotes JPanel und rechts das gelbe
        //splitpane.setLeftComponent(meinJDialog);
        spltPane.setRightComponent(downPnl);

        // Hier fÃ¼gen wir unserem Dialog unser JSplitPane hinzu
        add(spltPane);
        // Wir lassen unseren Dialog anzeigen
        setVisible(true);

        // starte in der mitte
        Dimension dmsn = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation(dmsn.width / 2 - this.getSize().width / 2, dmsn.height / 2 - this.getSize().height / 2);

        // reagiere auf key + button events

        registerHandlers();
    }


    private void registerHandlers() {
        controller.setChatMessageHandler(chatMessage -> {
                    chatArea.append(String.format("%s : %s \n", chatMessage.getName(), chatMessage.getText()));
                    chatArea.setCaretPosition(chatArea.getDocument().getLength());
                }
        );

        sendBtn.addActionListener(l -> {
            String text = sendArea.getText();
            if (text == null || text.trim().isEmpty()) {
                return;
            }
            var chatMessage = MsgChat.of(text, nameApp);
            controller.sendTextMessage(chatMessage);
            sendArea.setText("");
            sendArea.requestFocus(true);
            //chatMessagesSendArea.requestFocusInWindow();


        });
    }

}
