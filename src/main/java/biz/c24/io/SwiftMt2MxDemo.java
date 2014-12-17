package biz.c24.io;

import biz.c24.io.api.java8.C24;
import biz.c24.io.api.data.*;
import biz.c24.io.swift2008.MT103Message;
import biz.c24.io.transforms.swift.credittransfer.MT103_To_MXpacs00800101Transform;
import iso.std.iso.x20022.tech.xsd.pacs.x008.x001.x01.sdo.Document;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SwiftMt2MxDemo {
    private static List<String> swiftTextList = new ArrayList<>();
    private static List<SimpleDataObject> pacs008SdoList = new ArrayList<>();
    private static BlockingQueue<ComplexDataObject> exceptionQueue = new ArrayBlockingQueue<>(100);

    private static final ThreadLocal<MT103_To_MXpacs00800101Transform> transformThreadLocal =
            ThreadLocal.<MT103_To_MXpacs00800101Transform>withInitial(MT103_To_MXpacs00800101Transform::new);

    public static void main(String[] args) throws IOException, ValidationException, IOXPathException {
        SwiftMt2MxDemo swiftMt2MxDemo = new SwiftMt2MxDemo();
        swiftMt2MxDemo.execute();
    }

    private void execute() throws IOException {
        MT103Message mt103 = C24.parse(MT103Message.class, getClass().getResourceAsStream("/MT103i-valid_1.dat"));
        XORShift64 xorShift64 = new XORShift64(System.nanoTime());

        final int NUMBER = 50_000;

        Logger.getRootLogger().removeAllAppenders();
        Logger.getRootLogger().setLevel(Level.OFF);
        System.setProperty("biz.c24.io.api.data.SuppressExternalizedValidationWarnings", "true");        // This will suppress BIC and CCY warnings

        long start = 0;
        double duration = 0.0;

        Stream<String> mt103StringStream = Stream.generate(() -> {
            long random = xorShift64.randomLong();
            mt103.getBlock4().getSenderRef().getDefault().setReference(String.format("%016X", random));
            String mt103String = mt103.toString();
            if (random % 4001 == 0) {     // Roughly 0.025%
                mt103String = mt103String.replace(":USD", ":EUR");  // This will create an invalid message
            }
            return mt103String;
        });

        System.out.print("Creating test data...");
        swiftTextList = mt103StringStream.parallel().limit(NUMBER).collect(Collectors.toList());
        System.out.println("Done!");
        System.gc();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        ExceptionWorker workerThread = new ExceptionWorker(exceptionQueue);
        workerThread.setDaemon(true);
        executor.execute(workerThread);

        System.out.println("Parse, validate, transform, validate (again) and convert to SDO");
        start = System.nanoTime();
        pacs008SdoList = swiftTextList.parallelStream()
                .map(message -> parseMT103(message))
                .filter(mt103Message -> isValidMt103OrAddToQueue(mt103Message))
                .map(mt103Message -> transformMT2Pacs008(mt103Message))
                .map(pacs008 -> invalidateAFewPacs008(pacs008))
                .filter(pacs008 -> isValidPacs008OrAddToQueue(pacs008))
                .map(cdo -> toSdo(cdo))
                .collect(Collectors.toList());
        duration = (System.nanoTime() - start) / 1e9;
        System.out.printf("Time = %,d in %.2f seconds, %,.0f per second%n%n", NUMBER, duration, NUMBER / duration);

        try {
            executor.awaitTermination(1L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.out.println("Exception: " + e.getMessage());
        }

        System.gc();

        workerThread.terminate();
        executor.shutdown();

        System.out.println("size of pacs008SdoList = " + pacs008SdoList.size());
        iso.std.iso.x20022.tech.xsd.pacs.x008.x001.x01.sdo.Document sdo = (iso.std.iso.x20022.tech.xsd.pacs.x008.x001.x01.sdo.Document) pacs008SdoList.get(0);
        System.out.println("pacs008 time = " + sdo.getPacs00800101().getGrpHdr().getCreDtTm());
    }

    private static iso.std.iso.x20022.tech.xsd.pacs.x008.x001.x01.Document invalidateAFewPacs008(iso.std.iso.x20022.tech.xsd.pacs.x008.x001.x01.Document document) {
        if (document.getPacs00800101().getGrpHdr().getMsgId().substring(0, 3).equals("000"))
            document.getPacs00800101().getCdtTrfTxInf().get(0).getInstdAmt().setCcy("XXX");
        return document;
    }

    private static boolean isValidMt103OrAddToQueue(MT103Message mt103Message) {
        boolean isValid = false;

        if (mt103Message != null) {
            isValid = C24.isValid(mt103Message);

            if (!isValid) {
                try {
                    exceptionQueue.put(mt103Message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("Exception: " + e.getMessage());
                }
            }
        }
        return isValid;
    }

    private static boolean isValidPacs008OrAddToQueue(iso.std.iso.x20022.tech.xsd.pacs.x008.x001.x01.Document pacs008) {
        boolean isValid = false;

        if (pacs008 != null) {
            isValid = C24.isValid(pacs008);

            if (!isValid) {
                try {
                    exceptionQueue.put(pacs008);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("Exception: " + e.getMessage());
                }
            }
        }
        return isValid;
    }

    private static Document toSdo(iso.std.iso.x20022.tech.xsd.pacs.x008.x001.x01.Document cdo) {
        Document ret = null;
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

    private static iso.std.iso.x20022.tech.xsd.pacs.x008.x001.x01.Document transformMT2Pacs008(MT103Message mt103) {
        iso.std.iso.x20022.tech.xsd.pacs.x008.x001.x01.Document pacs008 = null;
        try {
            pacs008 = C24.transform(mt103, transformThreadLocal.get());
        } catch (ValidationException e) {
            System.out.println("Exception: " + e.getMessage());
        }
        return pacs008;
    }
}

class ExceptionWorker extends Thread {
    volatile boolean finished = false;
    BlockingQueue<ComplexDataObject> queue = null;

    public ExceptionWorker(BlockingQueue<ComplexDataObject> exceptionQueue) {
        queue = exceptionQueue;
    }

    @Override
    public void run() {
        ComplexDataObject message = null;

        while (!finished) {
            try {
                if (!queue.isEmpty()) {
                    message = queue.take();
                    List<ValidationEvent> validationEvents = Arrays.asList(C24.validateFully(message));
                    if (message instanceof MT103Message) {
                        System.out.print("MT103 failed:  \t");
                    } else if (message instanceof iso.std.iso.x20022.tech.xsd.pacs.x008.x001.x01.Document) {
                        System.out.print("Pacs008 failed:\t");
                    }
                    for (ValidationEvent event : validationEvents) {
                        System.out.print("\t" + event.getMessage());
                    }
                    System.out.println();
                } else
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

class XORShift64 {
    long x;

    public XORShift64(long seed) {
        x = seed == 0 ? 0xdeadbeef : seed;
    }

    public long randomLong() {
        x ^= (x << 21);
        x ^= (x >>> 35);
        x ^= (x << 4);
        return x;
    }
}
