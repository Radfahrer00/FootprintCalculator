import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.Instant;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MainApp {

    private static final String BROKER_URI = "tcp://192.168.56.1:1883";
    private static final String TOPIC_SUBSCRIBE = "footprint/average";
    private static final String TOPIC_PUBLISH = "footprint/userdata";

    private static JTextField globalAverageField;
    static MqttClient mqttClient;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> createAndShowGUI());
        try {
            mqttClient = new MqttClient(BROKER_URI, MqttClient.generateClientId(), new MemoryPersistence());
            mqttClient.connect();

            // Subscribe to a topic
            mqttClient.subscribe(TOPIC_SUBSCRIBE, (topic, message) -> {
                // Handle incoming messages from the subscribed topic
                updateGlobalAverageField(message);
            });

            //client.disconnect();
        } catch (MqttException ex) {
            ex.printStackTrace();
            System.out.println("No connection");
        }

    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Carbon Footprint Calculator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setPreferredSize(new Dimension(400, 200));

        // Panel for displaying global average
        JPanel globalAveragePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel globalAverageLabel = new JLabel("  Global average for today:");
        globalAverageField = new JTextField(10);
        globalAverageField.setEditable(false);
        globalAveragePanel.add(globalAverageLabel);
        globalAveragePanel.add(globalAverageField);
        mainPanel.add(globalAveragePanel, BorderLayout.NORTH);

        // Panel for entering user data
        JLabel headerLabel = new JLabel("Enter your consumption for the day:");
        headerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(headerLabel, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new GridLayout(6, 2));
        placeComponents(inputPanel);

        mainPanel.add(inputPanel, BorderLayout.SOUTH);

        frame.getContentPane().add(mainPanel);
        frame.pack();
        frame.setVisible(true);
    }

    private static void placeComponents(JPanel panel) {

        JLabel label1 = new JLabel("    Value 1:");
        JTextField value1Field = new JTextField(10);
        panel.add(label1);
        panel.add(value1Field);

        JLabel label2 = new JLabel("    Value 2:");
        JTextField value2Field = new JTextField(10);
        panel.add(label2);
        panel.add(value2Field);

        JLabel label3 = new JLabel("    Value 3:");
        JTextField value3Field = new JTextField(10);
        panel.add(label3);
        panel.add(value3Field);

        JLabel label4 = new JLabel("    Value 4:");
        JTextField value4Field = new JTextField(10);
        panel.add(label4);
        panel.add(value4Field);

        JLabel label5 = new JLabel("    Value 5:");
        JTextField value5Field = new JTextField(10);
        panel.add(label5);
        panel.add(value5Field);

        JButton submitButton = new JButton("Submit");
        panel.add(submitButton);

        submitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int value1 = Integer.parseInt(value1Field.getText());
                int value2 = Integer.parseInt(value2Field.getText());
                int value3 = Integer.parseInt(value3Field.getText());
                int value4 = Integer.parseInt(value4Field.getText());
                int value5 = Integer.parseInt(value5Field.getText());
                int result = value1 + value2 + value3 + value4 + value5;

                String messageToSend = getCurrentTime() + "_" + result;
                System.out.println("Message: " + messageToSend);


                // TODO: Send result to MQTT broker
                // Publish the result to another topic
                MqttMessage mqttMessage = new MqttMessage(messageToSend.getBytes());
                try {
                    mqttClient.publish(TOPIC_PUBLISH, mqttMessage);
                } catch (MqttException mqttException) {
                    mqttException.printStackTrace();
                }

                JOptionPane.showMessageDialog(panel, "Sum: " + result + "\n" + " Message to send: " + messageToSend, "Result", JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

    public static String getCurrentTime() {
        Instant instant = Instant.now();
        long currentTimeMillis = instant.toEpochMilli();
        //System.out.println("Current time: " + currentTimeMillis + " milliseconds");
        return String.valueOf(currentTimeMillis);
    }

    // Method to update the global average field with the incoming message
    private static void updateGlobalAverageField(MqttMessage mqttMessage) {
        String incomingMessage = new String(mqttMessage.getPayload());
        SwingUtilities.invokeLater(() -> globalAverageField.setText(incomingMessage));
    }
}

