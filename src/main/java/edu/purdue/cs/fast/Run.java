package edu.purdue.cs.fast;

import com.google.common.base.Stopwatch;
import edu.purdue.cs.fast.experiments.*;
import edu.purdue.cs.fast.config.CleanMethod;
import edu.purdue.cs.fast.models.DataObject;
import edu.purdue.cs.fast.models.Point;
import edu.purdue.cs.fast.models.Query;
import edu.purdue.cs.fast.models.Rectangle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

public class Run {
    public static Logger logger = LogManager.getLogger(Experiment.class);

    public static void main(String[] args) {
        String name = "fast_inc_new";
        if (args.length > 2)
            name = args[2];

        Workload workload = Workload.KNN;
        double srRate = 0.01;
        int k = 5;
        double knnRatio = 0.05;
        int numKeywords = 5;
        int numPreObjects = 0;
        int numObjects = 100000;
        int fineGridGran = 512;
        int maxLevel = 9;
        int maxRange = 512;
        boolean pushToLowest = false;
        CleanMethod cleanMethod = CleanMethod.NO;

        ArrayList<Integer> numQueriesList = new ArrayList<>();
        numQueriesList.add(100000);
        numQueriesList.add(500000);
        numQueriesList.add(1000000);
//        numQueriesList.add(2000000);
        numQueriesList.add(2500000);
        numQueriesList.add(5000000);
//        numQueriesList.add(10000000);
//        numQueriesList.add(20000000);

        for (int numQueries : numQueriesList) {
            FAST fast = new FAST(
                    new Rectangle(
                            new Point(0.0, 0.0),
                            new Point(maxRange, maxRange)
                    ),
                    fineGridGran,
                    maxLevel
            );
            FAST.config.INCREMENTAL_DESCENT = true;
            FAST.config.KNN_DEGRADATION_RATIO = 100;
            FAST.config.KNN_DEGRADATION_AR = 5.0;
            fast.setPushToLowest(pushToLowest);
            fast.setCleaning(cleanMethod);

            PlacesExperiment experiment;
            String ds = Paths.get(args[1], "data/places_dump_US.geojson").toString();
            switch (workload) {
                case MBR_EXPIRE:
                    experiment = new PlacesExpireExperiment(
                            Paths.get(args[0], "output_places_US_mbr_exp.csv").toString(),
                            ds, fast, getExpName(name, cleanMethod), numQueries, numObjects, numKeywords, srRate, maxRange
                    );
                    break;
                case HYBRID:
                    experiment = new PlacesHybridExperiment(
                            Paths.get(args[0], "output_places_US_hybrid.csv").toString(),
                            ds, fast, getExpName(name, cleanMethod), numQueries, numObjects, numKeywords, srRate, k, knnRatio, maxRange
                    );
                    break;
                case KNN:
                    experiment = new PlacesKNNExperiment(
                            Paths.get(args[0], "output_places_US_knn_inc_vs_direct.csv").toString(),
                            ds, fast, getExpName(name, cleanMethod), numPreObjects, 0, numQueries, numObjects, numKeywords, k, maxRange,
                            PlacesKNNExperiment.IndexType.FAST
                    );
                    break;
                case KNN_EXPIRE:
                    experiment = new PlacesKNNExpireExperiment(
                            Paths.get(args[0], "output_places_US_knn_exp2.csv").toString(),
                            ds, fast, getExpName(name, cleanMethod), numQueries, numObjects, numKeywords, k, maxRange,
                            PlacesKNNExperiment.IndexType.FAST
                    );
                    break;
                case KNN_OBJ_EXPIRE:
                    experiment = new PlacesKNNObjExpireExperiment(
                            Paths.get(args[0], "output_places_US_knn_obj_exp.csv").toString(),
                            ds, fast, getExpName(name, cleanMethod), numQueries, numObjects, numKeywords, k, maxRange,
                            PlacesKNNExperiment.IndexType.FAST
                    );
                    break;
                default:
                    experiment = new PlacesExperiment(
                            Paths.get(args[0], "output_places_US_mbr.csv").toString(),
                            ds, fast, getExpName(name, cleanMethod), numQueries, numObjects, numKeywords, srRate, maxRange
                    );
            }
            experiment.setSaveStats(true);
            experiment.run();

//            experiment.init();
//
//            System.gc();
//            System.gc();
//            System.gc();
//
//            Run.logger.info("Creating index!");
//            experiment.create();
//            saveQuerySnapshot(fast, 0);
//
//            Run.logger.info("Searching!");
//            int snapFrequency = 25000;
//            experiment.search((object) -> {
//                if (object.id % snapFrequency == 0 && object.id / snapFrequency > 0) {
//                    System.out.println("Saving snapshot!");
//                    saveQuerySnapshot(fast, object.id / snapFrequency);
//                }
//                return true;
//            });
//            saveQuerySnapshot(fast, 4);

//            try {
//                FileWriter fw = new FileWriter("results/" + getExpName(name, cleanMethod) + "_" + workload.name() + "_" + numQueries + ".txt");
//                BufferedWriter bw = new BufferedWriter(fw);
//
//                bw.write("id,ar,currentLevel,x,y\n");
//                fast.allKNNQueries().forEach(query -> {
//                    try {
//                        bw.write("" + query.id + ',' + query.ar + ',' + query.currentLevel + ',' + query.location.x + ',' + query.location.y + "\n");
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    }
//                });
//
//                bw.close();
//                fw.close();
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
        }
    }

    private static void saveQuerySnapshot(FAST fast, int timestamp) {
        try {
            FileWriter fw = new FileWriter("results/" + "query_snapshot_" + timestamp + ".csv");
            BufferedWriter bw = new BufferedWriter(fw);

            bw.write("id,ar,currentLevel,x,y\n");
            fast.allKNNQueries().forEach(query -> {
                try {
                    bw.write("" + query.id + ',' + query.ar + ',' + query.currentLevel + ',' + query.location.x + ',' + query.location.y + "\n");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            bw.close();
            fw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getExpName(String name, CleanMethod m) {
        return name + "_" + m.name();
    }

    private enum Workload {
        MBR,
        MBR_EXPIRE,
        HYBRID,
        KNN,
        KNN_EXPIRE,

        KNN_OBJ_EXPIRE,
    }
}
