import biz.c24.io.api.data.ValidationException;

import java.io.IOException;

/**
 * This class demonstrates some of the features that are available when deploying code with the Java 8 option.
 * It is expected that you are already familiar with GettingStartedCDO
 * Using a contrived example will highlight the useful features that are available if you deploy the code with the Java 8 option.
 * Note that the code was deployed with the C24 maven deploy task with the following property set - <javaVersion>JDK_8</javaVersion>
 * You could also achieve the same by setting the property in C24 IO Studio
 * The features you get with the Java 8 option are:
 * 1. java.time types
 * 2. Type safe collections
 * 3. Java enum API
 *
 */
public class WorkingWithJava8Extensions {

    public static void main(String[] args) throws IOException, ValidationException {

//        //First parse the xml data and create a CDO model that we can work with
//        PurchaseorderLocal purchaseOrder = C24.parse(PurchaseorderDocumentRoot.class).from(new File("/purchase-order.xml")).getPurchaseorder();
//
//        //Validate everything is correct
//        C24.validate(purchaseOrder);
//
//        //In the standard C24 deployment ISO8601 Date and Time types are mapped as custom C24 Date and Time types except for Generic Date which is mapped to java.util.date
//        //Using the Java 8 deploy option Date and time types will be mapped to their equivalents in the java.time package
//
//        //ISO8601 DateTime will be mapped to java.time.ZonedDateTime
//        ZonedDateTime purchaseDate = purchaseOrder.getPurchasedate();
//
//        //This allows us to use the powerful features in the java.time API
//        StringBuffer sb = new StringBuffer();
//        sb.append("Order placed on ").append(purchaseDate.getDayOfMonth());
//        sb.append(" of ").append(purchaseDate.getMonth());
//        sb.append(" ").append(purchaseDate.getYear());
//        ZonedDateTime today = ZonedDateTime.now(purchaseDate.getZone());
//        sb.append(", ").append(ChronoUnit.DAYS.between(purchaseDate.toLocalDateTime(), today.toLocalDateTime()));
//        sb.append(" days ago");
//        sb.append("\n");
//        System.out.print(sb);
//        System.out.println();
//
//        //ISO8601 Date will be mapped to java.time.LocalDate
//        LocalDate shippingDate = purchaseOrder.getShippingdate();
//        System.out.println("Items will be shipped on " + shippingDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)));
//        System.out.println();
//
//        //ISO8061 Time will be mapped to java.time.LocalTime
//        LocalTime deliveryTimeSlot = purchaseOrder.getDeliverytimeslot();
//        System.out.println("Preferred Delivery Time: " + deliveryTimeSlot.format(DateTimeFormatter.ISO_TIME));
//        System.out.println();
//
//        //other type mappings
//        // ISO8601 Duration -> java.time.Duration
//        // ISO8601 Year -> java.time.Year
//        // ISO8601 MonthDay -> java.time.MonthDay
//        // ISO8601 YearMonth -> java.time.YearMonth
//
//        //In the standard C24 deployment collections are represented as arrays
//        // e.g. LineItem[] lineItems
//        //Deploying with the Java 8 option will generate type safe collections
//
//        List<Lineitem> lineItems = purchaseOrder.getLineitems();
//
//        Double invoiceTotal = lineItems.stream().mapToDouble(item -> item.getPrice() * item.getQuantity()).sum();
//        System.out.println("Invoice Total: " + String.format( "%.2f", invoiceTotal));
//        System.out.println();
//
//        //Any types that are backed by an enumeration will have that enumeration exposed via a getter and setter
//        //There won't be any changes to the underlying property and it will still contain the usual enumeration validator
//
//        //the property is a String
//        String currency = lineItems.get(0).getCurrency();
//        System.out.println("The currency for Line Item 1 is " + currency);
//
//        //It has a corresponding enum getter based on the property name with an "enum" suffix
//        //The generated enum class will have a suffix of "JavaEnum" to differentiate it from the C24 generated EnumType class
//        CurrencyCodeJavaEnum currencyEnum = lineItems.get(0).getCurrencyEnum();
//        System.out.println("getCurrencyEnum() for Line Item 1 returns " + currencyEnum.getValue());
//
//        //It also has a corresponding enum setter
//        lineItems.get(0).setCurrencyEnum(CurrencyCodeJavaEnum.EUR);
//        //setting the enum will also change the underlying property
//        System.out.println("the currency for Line Item 1 has been changed to " + lineItems.get(0).getCurrency());
//
//        //we can also generate SDO objects using the JDK_8 option
//        //However we will need to use the java8.C24 class that knows how to handle these options
//        biz.c24.io.gettingstarted.purchaseorder.sdo.PurchaseorderLocal  sdoPO = biz.c24.io.api.java8.C24.toSdo(purchaseOrder);
//
//        //this will populate the same java.time properties as above
//        ZonedDateTime purchaseDateFromSdo = sdoPO.getPurchasedate();
//        System.out.println("The purchase date property is a java.time.ZonedDateTime instance: " + purchaseDateFromSdo.format(DateTimeFormatter.ISO_DATE_TIME));

    }


}
