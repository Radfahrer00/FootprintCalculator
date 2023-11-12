import javax.swing.*;
import java.awt.*;
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
        SwingUtilities.invokeLater(MainApp::createAndShowGUI);
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
        mainPanel.setPreferredSize(new Dimension(550, 440));

        // Panel for displaying global average
        JPanel globalAveragePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        globalAveragePanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0)); // Add vertical space
        JLabel globalAverageLabel = new JLabel("Global average for today:");
        globalAverageField = new JTextField(10);
        globalAverageField.setEditable(false);
        globalAveragePanel.add(globalAverageLabel);
        globalAveragePanel.add(globalAverageField);
        mainPanel.add(globalAveragePanel, BorderLayout.NORTH);

        // Panel for entering user data
        JLabel headerLabel = new JLabel("Enter your consumption for the day:");
        headerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(headerLabel, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new GridLayout(11, 2));
        placeComponents(inputPanel);

        mainPanel.add(inputPanel, BorderLayout.SOUTH);

        frame.getContentPane().add(mainPanel);
        frame.pack();
        frame.setVisible(true);
    }

    private static void placeComponents(JPanel panel) {

        JLabel label1 = new JLabel("Beef (in grams):");
        JTextField value1Field = new JTextField(10);
        panel.add(label1);
        panel.add(value1Field);

        JLabel label2 = new JLabel("Pork (in gram):");
        JTextField value2Field = new JTextField(10);
        panel.add(label2);
        panel.add(value2Field);

        JLabel label3 = new JLabel("Chicken (in gram):");
        JTextField value3Field = new JTextField(10);
        panel.add(label3);
        panel.add(value3Field);

        JLabel label4 = new JLabel("Fish (in gram):");
        JTextField value4Field = new JTextField(10);
        panel.add(label4);
        panel.add(value4Field);

        JLabel label5 = new JLabel("Butter (in gram):");
        JTextField value5Field = new JTextField(10);
        panel.add(label5);
        panel.add(value5Field);

        JLabel label6 = new JLabel("Other dairy products (in gram):");
        JTextField value6Field = new JTextField(10);
        panel.add(label6);
        panel.add(value6Field);

        JLabel label7 = new JLabel("Car (in km):");
        JTextField value7Field = new JTextField(10);
        panel.add(label7);
        panel.add(value7Field);

        JLabel label8 = new JLabel("Public transport (in km):");
        JTextField value8Field = new JTextField(10);
        panel.add(label8);
        panel.add(value8Field);

        JLabel label9 = new JLabel("Plane (in km):");
        JTextField value9Field = new JTextField(10);
        panel.add(label9);
        panel.add(value9Field);

        JLabel label10 = new JLabel("<html>Electrical appliances,<br>e.g. washing machine (in number of uses):</html>");
        JTextField value10Field = new JTextField(10);
        panel.add(label10);
        panel.add(value10Field);

        JButton submitButton = new JButton("Submit");
        panel.add(submitButton);

        submitButton.addActionListener(e -> {
            int beef = Integer.parseInt(value1Field.getText());
            int pork = Integer.parseInt(value2Field.getText());
            int chicken = Integer.parseInt(value3Field.getText());
            int fish = Integer.parseInt(value4Field.getText());
            int butter = Integer.parseInt(value5Field.getText());
            int dairyProducts = Integer.parseInt(value6Field.getText());
            int car = Integer.parseInt(value7Field.getText());
            int pTransport = Integer.parseInt(value8Field.getText());
            int plane = Integer.parseInt(value9Field.getText());
            int electricalAppliances = Integer.parseInt(value10Field.getText());

            float result = calculateConsumption(beef, pork, chicken, fish, butter, dairyProducts, car,
                    pTransport, plane, electricalAppliances);

            String messageToSend = getCurrentTime() + "_" + result;
            System.out.println("Message: " + messageToSend);


            // Publish the result to topic
            MqttMessage mqttMessage = new MqttMessage(messageToSend.getBytes());
            try {
                mqttClient.publish(TOPIC_PUBLISH, mqttMessage);
            } catch (MqttException mqttException) {
                mqttException.printStackTrace();
            }

            String averageComparison = "The average for today is: ";
            if (!globalAverageField.getText().isEmpty()) {
                // Convert the string to a floating-point number
                double averageValue = Double.parseDouble(globalAverageField.getText());
                if (result < averageValue) {
                    averageComparison += averageValue + ". You have consumed less than the average. Nice!";
                } else if (result > averageValue) {
                    averageComparison += averageValue + ". You have consumed more than the average.\n" +
                            "Try to lower your consumption!";
                } else {
                    averageComparison += averageValue + ". You have consumed exactly the average.";
                }
            } else {
                averageComparison = "You are the first to publish your data for the day.\n" +
                        "Come back later to compare yourself to the average.";
            }

            JOptionPane.showMessageDialog(panel, "Sum: " + result + "\n" +
                    averageComparison, "Result", JOptionPane.INFORMATION_MESSAGE);
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
        // Convert the string to a floating-point number
        double incomingValue = Double.parseDouble(incomingMessage);
        // Format the number to a string with two decimal places
        String formattedValue = String.format("%.2f", incomingValue);
        SwingUtilities.invokeLater(() -> globalAverageField.setText(formattedValue));
    }

    private static float calculateConsumption(int beef, int pork, int chicken, int fish, int butter, int dairyProducts, int carKm,
                                              int pTransportKm, int planeKm, int electricalAppliances) {
        float beefConsumption = (float) (beef * 16.88);
        float porkConsumption = (float) (pork * 6.92);
        float chickenConsumption = (float) (chicken * 2.79);
        float fishConsumption = (float) (fish * 5.14);
        float butterConsumption = (float) (butter * 12.11);
        float dairyProductsConsumption = (float) (dairyProducts * 5.89);
        float carConsumption = carKm * 171;
        float pTransportConsumption = pTransportKm * 67;
        float planeConsumption = planeKm * 365;
        float appliancesConsumption = electricalAppliances * 750;

        return beefConsumption + porkConsumption + chickenConsumption + fishConsumption + butterConsumption
                + dairyProductsConsumption + carConsumption + pTransportConsumption + planeConsumption + appliancesConsumption;
    }
}

// Source for CO2 Consumption Values: https://interaktiv.tagesspiegel.de/lab/wie-klimaschaedlich-sind-beliebte-lebensmittel/