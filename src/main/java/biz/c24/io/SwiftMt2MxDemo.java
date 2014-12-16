package biz.c24.io;

import biz.c24.io.api.java8.C24;
import biz.c24.io.api.data.*;
import biz.c24.io.swift2008.MT103Message;
import biz.c24.io.transforms.swift.credittransfer.MT103_To_MXpacs00800101Transform;
import iso.std.iso.x20022.tech.xsd.pacs.x008.x001.x01.Document;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SwiftMt2MxDemo {
    private static List<String> swiftTextList = new ArrayList<>();
    private static List<SimpleDataObject> pacs008SdoList = new ArrayList<>();
    private static BlockingQueue<MT103Message> exceptionQueue = new ArrayBlockingQueue<>(10);

    private static final ThreadLocal<MT103_To_MXpacs00800101Transform> transformThreadLocal =
            ThreadLocal.<MT103_To_MXpacs00800101Transform>withInitial(MT103_To_MXpacs00800101Transform::new);

    public static void main(String[] args) throws IOException, ValidationException, IOXPathException {
        SwiftMt2MxDemo swiftMt2MxDemo = new SwiftMt2MxDemo();
       swiftMt2MxDemo.execute();
    }

    public void execute() throws IOException {
        MT103Message mt103 = C24.parse(MT103Message.class, getClass().getResourceAsStream("/MT103i-valid_1.dat"));
        AtomicInteger atomicInt = new AtomicInteger(0);

        final int NUMBER = 10_000;

        Logger.getRootLogger().removeAllAppenders();
        Logger.getRootLogger().setLevel(Level.OFF);
        System.setProperty("biz.c24.io.api.data.SuppressExternalizedValidationWarnings", "true");        // This will suppress BIC and CCY warnings

        long start = 0;
        double duration = 0.0;

        Stream<String> mt103StringStream = Stream.generate(() -> {
            int i = atomicInt.incrementAndGet();
            mt103.getBlock4().getSenderRef().getDefault().setReference(String.format("%06d", i));
            String mt103String = mt103.toString();
            if (i % 401 == 0) {     // Roughly 0.25%
                mt103String = mt103String.replace(":USD", ":EUR");  // This will create an invalid message
            }
            return mt103String;
        });

        System.out.println("Creating test data...");
        swiftTextList = mt103StringStream.parallel().limit(NUMBER).collect(Collectors.toList());
        System.out.println("Done!");
        System.gc();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        ExceptionWorker workerThread = new ExceptionWorker(exceptionQueue);
        workerThread.setDaemon(true);
        executor.execute(workerThread);

        try {
            executor.awaitTermination(1L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.out.println("Exception: " + e.getMessage());
        }

        System.out.print("Warming up the JIT...");
        pacs008SdoList = SwiftMt2MxDemo.swiftTextList.parallelStream()
                .map(SwiftMt2MxDemo::parseMT103)
                .filter(SwiftMt2MxDemo::isValidOrAddToQueue)
                .map(SwiftMt2MxDemo::transformMT2Pacs008)
                .map(SwiftMt2MxDemo::toSdo)
                .collect(Collectors.toList());
        System.out.println("Done!");
        pacs008SdoList.clear();

        System.gc();

        System.out.println("Starting full parse and validate...");
        start = System.nanoTime();
        pacs008SdoList = SwiftMt2MxDemo.swiftTextList.parallelStream()
                .map(SwiftMt2MxDemo::parseMT103)
                .filter(SwiftMt2MxDemo::isValidOrAddToQueue)
                .map(SwiftMt2MxDemo::transformMT2Pacs008)
                .map(SwiftMt2MxDemo::toSdo)
                .collect(Collectors.toList());
        duration = (System.nanoTime() - start) / 1e9;
        System.out.printf("Time = %,d in %.2f seconds, %,.0f per second%n%n", NUMBER, duration, NUMBER / duration);

        System.gc();

        workerThread.terminate();
        executor.shutdown();

        System.out.println("size of pacs008SdoList = " + pacs008SdoList.size());
        System.out.println("size of exception queue = " + exceptionQueue.size());
        iso.std.iso.x20022.tech.xsd.pacs.x008.x001.x01.sdo.Document sdo = (iso.std.iso.x20022.tech.xsd.pacs.x008.x001.x01.sdo.Document) pacs008SdoList.get(0);
        System.out.println("pacs008 time = " + sdo.getPacs00800101().getGrpHdr().getCreDtTm());
        System.out.println("sdo.getSdoData().array().length = " + sdo.getSdoData().array().length);
        dump(sdo.getSdoData().array());
        System.out.println("C24.toCdo(sdo) = " + C24.toCdo(sdo));
    }

    private static boolean isValidOrAddToQueue(MT103Message mt103Message) {
        boolean isValid = C24.isValid(mt103Message);

        if (!isValid) {
            try {
                exceptionQueue.put(mt103Message);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Exception: " + e.getMessage());
            }
        }
        return isValid;
    }

    private static SimpleDataObject toSdo(ComplexDataObject cdo) {
        SimpleDataObject ret = null;
        try {
            ret = C24.toSdo(cdo);
        } catch (IOException e) {
            System.out.println("Exception: " + e.getMessage());
        }
        return ret;
    }

    private static MT103Message parseMT103(String message) {
        MT103Message mt103 = null;
        try {
            mt103 = C24.parse(MT103Message.class, new StringReader(message));
        } catch (IOException e) {
            System.out.println("Exception: " + e.getMessage());
        }
        return mt103;
    }

    private static Document transformMT2Pacs008(MT103Message mt103) {
        Document pacs008 = null;
        try {
            pacs008 = C24.transform(mt103, transformThreadLocal.get());
        } catch (ValidationException e) {
            System.out.println("Exception: " + e.getMessage());
        }
        return pacs008;
    }

    private static void dump(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            if (i % 64 == 0) {
                System.out.printf("%n%04d...%04d:\t",(i), (i+63) );
            }
            char c = (char) bytes[i];
            System.out.print((c < '~' && c >= ' ' ? c : '.'));
        }
        System.out.println();
    }
}

class ExceptionWorker extends Thread {
    volatile boolean finished = false;
    BlockingQueue<MT103Message> queue = null;

    public ExceptionWorker(BlockingQueue<MT103Message> exceptionQueue) {
        queue = exceptionQueue;
    }

    @Override
    public void run() {
        MT103Message message = null;

        while ( ! finished ) {
            try {
                if( !queue.isEmpty() ) {
                    message = queue.take();
                    List<ValidationEvent> validationEvents = Arrays.asList(C24.validateFully(message));
//                    System.out.print("Exception for message: " + message.getBlock4().getSenderRef());
//                    for( ValidationEvent event : validationEvents ) {
//                        System.out.println("event = " + event.getMessage());
//                    }
                }
                else
                    continue;
            } catch (InterruptedException e) {
                System.out.println("Exception: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("Finished exception worker thread :-)");
    }

    public void terminate() {
        this.finished = true;
    }

}
