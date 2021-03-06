package main;

import generatedOrder.GetOrderRequest;
import generatedRestaurantsResponse.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final String restaurantsServlet = "http://localhost:8080/restaurants";
    private static final String orderServlet = "http://localhost:8080/order";

    public static void main(String[] args) {
        new Main().run();
    }

    public void run() {
        try {
            generatedRestaurantsResponse.Envelope restaurants = getRestaurants();
            List<Integer> selection = showMenu(restaurants.getBody().getGetRestaurantsResponse());
            submitOrder(selection, restaurants.getBody().getGetRestaurantsResponse());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    public HttpURLConnection createConnection(String url, String methodType) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) (new URL(url)).openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestMethod(methodType);
        return connection;
    }

    public generatedRestaurantsResponse.Envelope getRestaurants() throws IOException, JAXBException {
        HttpURLConnection connection = createConnection(restaurantsServlet, "POST");

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        File file = new File("blah.xml");
        BufferedWriter out = new BufferedWriter(new FileWriter(file));

        String input;
        while ((input = in.readLine()) != null) out.write(input);
        out.close();
        connection.disconnect();

        JAXBContext jaxbContext = JAXBContext.newInstance(generatedRestaurantsResponse.Envelope.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

        return (Envelope) unmarshaller.unmarshal(file);
    }

    public List<Integer> showMenu(GetRestaurantsResponse restaurants) {

        // Get Restaurant
        for (int i = 0; i < restaurants.getRestaurant().size(); i++) {
            System.out.println(i + ". " + restaurants.getRestaurant().get(i).getTitle());
        }
        Scanner scan = new Scanner(System.in);
        Integer restaurantIndex = scan.nextInt();

        // Get Menu Item
        for(int i = 0; i < restaurants.getRestaurant().get(restaurantIndex).getMenu().getItem().size(); i++) {
            Item item = restaurants.getRestaurant().get(restaurantIndex).getMenu().getItem().get(i);
            System.out.println(i + ". " + item.getName() + " ($" + item.getPrice() + ")");
        }
        Integer itemIndex = scan.nextInt();

        List<Integer> list = new ArrayList<Integer>();
        list.add(restaurantIndex);
        list.add(itemIndex);
        return list;
    }

    public void submitOrder(List<Integer> selection, GetRestaurantsResponse restaurants) throws IOException, JAXBException {
        int restaurantIndex = selection.get(0);
        int itemIndex = selection.get(1);

        GetOrderRequest myOrder = new GetOrderRequest();

        // Create SOAP envelope & body
        generatedOrder.Envelope envelope = new generatedOrder.Envelope();
        generatedOrder.Body body = new generatedOrder.Body();

        // Create ORDERED restaurant
        generatedOrder.Restaurant restaurant = new generatedOrder.Restaurant();
        restaurant.setMenu(new generatedOrder.Menu());

        // Create ORDERED menu
        generatedOrder.Menu menu = new generatedOrder.Menu();

        // Create menu item
        generatedOrder.Item item = new generatedOrder.Item();

        // Get ITEM
        item.setName(restaurants.getRestaurant().get(restaurantIndex).getMenu().getItem().get(itemIndex).getName());

        // Add
        menu.setItem(item);
        restaurant.setMenu(menu);
        myOrder.setRestaurant(restaurant);
        body.setGetOrderRequest(myOrder);
        envelope.setBody(body);

        // Connect to server
        HttpURLConnection connection = createConnection(orderServlet, "POST");
        connection.setRequestProperty("Connection", "Keep-Alive");
        connection.setRequestProperty("Protocol-Version", "1.1");
        connection.setRequestProperty("Content-Type", "text/xml");

        OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());

        JAXBContext jaxbContext = JAXBContext.newInstance(generatedOrder.Envelope.class);
        Marshaller marshaller = jaxbContext.createMarshaller();

        marshaller.marshal(envelope, out);


        out.flush();
        out.close();
        System.out.println("Response code: " + connection.getResponseCode());
        connection.disconnect();

    }
}
