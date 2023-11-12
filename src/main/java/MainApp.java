import javax.swing.*;
import java.awt.*;
import java.time.Instant;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * Main application class for the Carbon Footprint Calculator.
 */
public class MainApp {

    // MQTT broker configurations
    private static final String BROKER_URI = "tcp://192.168.56.1:1883";
    private static final String TOPIC_SUBSCRIBE = "footprint/average";
    private static final String TOPIC_PUBLISH = "footprint/userdata";
    static MqttClient mqttClient;

    // UI components
    private static JTextField globalAverageField;
    private static JTextField value1Field, value2Field, value3Field, value4Field, value5Field, value6Field, value7Field, value8Field, value9Field, value10Field;

    /**
     * Main method to start the application.
     * @param args Command-line arguments (not used).
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainApp::createAndShowGUI);
        try {
            mqttClient = new MqttClient(BROKER_URI, MqttClient.generateClientId(), new MemoryPersistence());
            mqttClient.connect();

            // Subscribe to a topic
            mqttClient.subscribe(TOPIC_SUBSCRIBE, (topic, message) -> {
                // Handle incoming messages from the subscribed topic
                updateGlobalAverageField(message);

                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        if (mqttClient != null && mqttClient.isConnected()) {
                            mqttClient.disconnect();
                        }
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }));
            });
        } catch (MqttException ex) {
            ex.printStackTrace();
            System.out.println("No connection");
        }
    }

    /**
     * Creates and shows the graphical user interface.
     */
    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Carbon Footprint Calculator");
        frame.setFont(new Font("SansSerif", Font.PLAIN, 18));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setPreferredSize(new Dimension(600, 440));

        // Create and add the panel displaying global average and the panel for user input
        createGlobalAveragePanel(mainPanel);
        createUserInputPanel(mainPanel);

        frame.getContentPane().add(mainPanel);
        frame.pack();
        frame.setVisible(true);
    }

    /**
     * Creates the panel displaying the global average.
     * @param mainPanel The main panel to which the global average panel is added.
     */
    private static void createGlobalAveragePanel(JPanel mainPanel) {
        JPanel globalAveragePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        globalAveragePanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0)); // Add vertical space
        JLabel globalAverageLabel = new JLabel("Global average for today:");
        globalAverageLabel.setFont(new Font("SansSerif", Font.PLAIN, 15));
        globalAverageField = new JTextField(10);
        globalAverageField.setFont(new Font("SansSerif", Font.PLAIN, 15));
        globalAverageField.setEditable(false);
        globalAveragePanel.add(globalAverageLabel);
        globalAveragePanel.add(globalAverageField);
        mainPanel.add(globalAveragePanel, BorderLayout.NORTH);
    }

    /**
     * Creates the panel for user input.
     * @param mainPanel The main panel to which the user input panel is added.
     */
    private static void createUserInputPanel(JPanel mainPanel) {
        JLabel headerLabel = new JLabel("Enter your consumption for the day:");
        headerLabel.setFont(new Font("SansSerif", Font.PLAIN, 15));
        headerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(headerLabel, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new GridLayout(11, 2));
        placeComponents(inputPanel);

        mainPanel.add(inputPanel, BorderLayout.SOUTH);
    }

    /**
     * Places the input components on the specified panel.
     * @param panel The panel to which the input components are added.
     */
    private static void placeComponents(JPanel panel) {
        // Create and add labels and text fields for user input
        JLabel label1 = createLabel("Beef (in gram):");
        value1Field = new JTextField(10);
        panel.add(label1);
        panel.add(value1Field);

        JLabel label2 = createLabel("Pork (in gram):");
        value2Field = new JTextField(10);
        panel.add(label2);
        panel.add(value2Field);

        JLabel label3 = createLabel("Chicken (in gram):");
        value3Field = new JTextField(10);
        panel.add(label3);
        panel.add(value3Field);

        JLabel label4 = createLabel("Fish (in gram):");
        value4Field = new JTextField(10);
        panel.add(label4);
        panel.add(value4Field);

        JLabel label5 = createLabel("Butter (in gram):");
        value5Field = new JTextField(10);
        panel.add(label5);
        panel.add(value5Field);

        JLabel label6 = createLabel("Other dairy products (in gram):");
        value6Field = new JTextField(10);
        panel.add(label6);
        panel.add(value6Field);

        JLabel label7 = createLabel("Car (in km):");
        value7Field = new JTextField(10);
        panel.add(label7);
        panel.add(value7Field);

        JLabel label8 = createLabel("Public transport (in km):");
        value8Field = new JTextField(10);
        panel.add(label8);
        panel.add(value8Field);

        JLabel label9 = createLabel("Plane (in km):");
        value9Field = new JTextField(10);
        panel.add(label9);
        panel.add(value9Field);

        JLabel label10 = createLabel("<html>Electrical appliances,<br>e.g. washing machine (in number of uses):</html>");
        value10Field = new JTextField(10);
        panel.add(label10);
        panel.add(value10Field);

        // Create and add the submit button
        JButton submitButton = new JButton("Submit");
        submitButton.setFont(new Font("SansSerif", Font.PLAIN, 13));
        panel.add(submitButton);

        // Add action listener for the submit button
        handleSubmitButton(panel, submitButton);
    }

    /**
     * Creates a labeled JLabel with specified text and formatting.
     * @param labelString The text for the label.
     * @return The created JLabel.
     */
    private static JLabel createLabel(String labelString) {
        JLabel label = new JLabel(labelString);
        label.setFont(new Font("SansSerif", Font.PLAIN, 13));
        label.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        return label;
    }

    /**
     * Adds action listener to the submit button.
     * @param panel        The panel to which the submit button is added.
     * @param submitButton The submit button.
     */
    private static void handleSubmitButton(JPanel panel, JButton submitButton) {
        submitButton.addActionListener(e -> {
            float result = -1;
            result = collectInput(result);

            if (result != -1) {
                String messageToSend = getCurrentTime() + "_" + result;
                System.out.println("Message: " + messageToSend);

                publishMessage(messageToSend);

                String averageComparison = compareToAverage(result);

                JOptionPane.showMessageDialog(panel, "Total consumption today: " + result + " grams of CO2\n" +
                        averageComparison, "Result", JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

    /**
     * Collects user input from text fields and calculates the consumption.
     * @param result The initial result value.
     * @return The calculated consumption value.
     */
    private static float collectInput(float result) {
        try {
            // Parse user input from text fields
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

            // Calculate consumption based on user input
            result = calculateConsumption(beef, pork, chicken, fish, butter, dairyProducts, car,
                    pTransport, plane, electricalAppliances);
        } catch (NumberFormatException exception) {
            JOptionPane.showMessageDialog(null, "Please enter valid whole numbers in all fields.", "Input Error", JOptionPane.ERROR_MESSAGE);
        }
        return result;
    }

    /**
     * Publishes the calculated message to the MQTT broker.
     * @param messageToSend The message to be published.
     */
    private static void publishMessage(String messageToSend) {
        // Publish the result to topic
        MqttMessage mqttMessage = new MqttMessage(messageToSend.getBytes());
        try {
            mqttClient.publish(TOPIC_PUBLISH, mqttMessage);
        } catch (MqttException mqttException) {
            mqttException.printStackTrace();
        }
    }

    /**
     * Compares the user's consumption to the global average.
     * @param result The user's calculated consumption.
     * @return A string indicating the comparison result.
     */
    private static String compareToAverage(float result) {
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
        return averageComparison;
    }

    /**
     * Calculates the CO2 consumption based on user input.
     * @param beef                 Grams of beef consumed.
     * @param pork                 Grams of pork consumed.
     * @param chicken              Grams of chicken consumed.
     * @param fish                 Grams of fish consumed.
     * @param butter               Grams of butter consumed.
     * @param dairyProducts        Grams of other dairy products consumed.
     * @param carKm                Kilometers traveled by car.
     * @param pTransportKm         Kilometers traveled by public transport.
     * @param planeKm              Kilometers traveled by plane.
     * @param electricalAppliances Number of uses of electrical appliances.
     * @return The calculated CO2 consumption.
     */
    private static float calculateConsumption(int beef, int pork, int chicken, int fish, int butter, int dairyProducts, int carKm,
                                              int pTransportKm, int planeKm, int electricalAppliances) {
        // Calculate consumption for each category
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

        // Sum up the consumptions to get the total CO2 consumption
        return beefConsumption + porkConsumption + chickenConsumption + fishConsumption + butterConsumption
                + dairyProductsConsumption + carConsumption + pTransportConsumption + planeConsumption + appliancesConsumption;
    }

    /**
     * Gets the current time in milliseconds.
     * @return The current time in milliseconds.
     */
    private static String getCurrentTime() {
        Instant instant = Instant.now();
        long currentTimeMillis = instant.toEpochMilli();
        //System.out.println("Current time: " + currentTimeMillis + " milliseconds");
        return String.valueOf(currentTimeMillis);
    }

    /**
     * Updates the global average field with the incoming MQTT message.
     * @param mqttMessage The incoming MQTT message.
     */
    private static void updateGlobalAverageField(MqttMessage mqttMessage) {
        String incomingMessage = new String(mqttMessage.getPayload());
        double incomingValue = Double.parseDouble(incomingMessage);
        // Format the number to a string with two decimal places
        String formattedValue = String.format("%.2f", incomingValue);
        SwingUtilities.invokeLater(() -> globalAverageField.setText(formattedValue));
    }
}

// Source for CO2 Consumption Values: https://interaktiv.tagesspiegel.de/lab/wie-klimaschaedlich-sind-beliebte-lebensmittel/