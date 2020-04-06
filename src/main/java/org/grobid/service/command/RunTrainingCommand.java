package org.grobid.service.command;

import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.grobid.core.engines.Engine;
import org.grobid.core.engines.QuantitiesModels;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.main.GrobidHomeFinder;
import org.grobid.core.main.LibraryLoader;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.service.configuration.GrobidQuantitiesConfiguration;
import org.grobid.trainer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

public class RunTrainingCommand extends ConfiguredCommand<GrobidQuantitiesConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RunTrainingCommand.class);
    private final static String ACTION = "action";
    private final static String PRINT = "print";
    private final static String RECURSIVE = "recursive";
    private final static String FOLD_TYPE = "foldType";
    private final static String FOLD_COUNT = "foldCount";
    private final static String MODEL_NAME = "model";

    private final static List<String> ACTIONS = Arrays.asList("train", "nfold", "train_eval", "holdout");

    Map<String, AbstractTrainer> trainerMap = new HashMap<>();


    public RunTrainingCommand() {
        super("training", "Training / Evaluate the model ");

        trainerMap.put(QuantitiesModels.QUANTITIES.getModelName(), new QuantitiesTrainer());
        trainerMap.put(QuantitiesModels.UNITS.getModelName(), new UnitTrainer());
        trainerMap.put(QuantitiesModels.VALUES.getModelName(), new ValueTrainer());
        trainerMap.put(QuantitiesModels.QUANTIFIED_OBJECT.getModelName(), new QuantifiedObjectTrainer());
    }

    @Override
    public void configure(Subparser subparser) {
        super.configure(subparser);

        subparser.addArgument("-a", "--action")
            .dest(ACTION)
            .type(String.class)
            .required(false)
            .choices(ACTIONS)
            .setDefault("train")
            .help("Actions to the training command. ");

        subparser.addArgument("-m", "--model")
            .dest(MODEL_NAME)
            .type(String.class)
            .required(false)
            .setDefault("quantities")
            .help("Model to train");

        subparser.addArgument("-op", "--onlyPrint")
            .dest(PRINT)
            .type(Boolean.class)
            .required(false)
            .setDefault(Boolean.FALSE)
            .help("Print on screen instead of writing on a log file");

        subparser.addArgument("-fc", "--fold-count")
            .dest(FOLD_COUNT)
            .type(Integer.class)
            .required(false)
            .setDefault(10)
            .help("Specify if the number of fold in n-fold cross-validation. ");

    }

    @Override
    protected void run(Bootstrap bootstrap, Namespace namespace, GrobidQuantitiesConfiguration configuration) throws Exception {
        try {
            GrobidProperties.set_GROBID_HOME_PATH(new File(configuration.getGrobidHome()).getAbsolutePath());
            String grobidHome = configuration.getGrobidHome();
            if (grobidHome != null) {
                GrobidProperties.setGrobidPropertiesPath(new File(grobidHome, "/config/grobid.properties").getAbsolutePath());
            }

            GrobidHomeFinder grobidHomeFinder = new GrobidHomeFinder(Arrays.asList(configuration.getGrobidHome()));
            GrobidProperties.getInstance(grobidHomeFinder);
            Engine.getEngine(false);
            LibraryLoader.load();
        } catch (final Exception exp) {
            System.err.println("Grobid initialisation failed, cannot find Grobid Home. Maybe you forget to specify the config.yml in the command launch?");
            exp.printStackTrace();

            System.exit(-1);
        }

        String modelName = namespace.get(MODEL_NAME);
        String action = namespace.get(ACTION);
        Boolean print = namespace.get(PRINT);
        Integer foldCount = namespace.get(FOLD_COUNT);

        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");


        Trainer trainer = trainerMap.get(modelName);
        if (trainer != null) {
            System.out.println(super.getDescription());
            String name = "";
            String report = null;

            switch (action) {
                case "train":
                    AbstractTrainer.runTraining(trainer);
                    break;
                case "nfold":
                    report = AbstractTrainer.runNFoldEvaluation(trainer, foldCount, true);
                    name = foldCount + "-fold-cross-validation";
                    break;
                case "train_eval":
                    report = AbstractTrainer.runSplitTrainingEvaluation(trainer, 0.8);
                    name = "80-20-evaluation";
                    break;
                case "holdout":
                    AbstractTrainer.runTraining(trainer);
                    report = AbstractTrainer.runEvaluation(trainer, true);
                    name = "holdout-evaluation";
                    break;
                default:
                    System.out.println("No correct action were supplied. Please provide beside " + Arrays.toString(ACTIONS.toArray()));
                    break;

            }
            if (report != null) {
                if (!print) {
                    if (!Files.exists(Paths.get("logs"))) {
                        Files.createDirectory(Paths.get("logs"));
                    }

                    try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("logs/" + modelName + "-" + name
                        + "-" + formatter.format(date) + ".txt"))) {
                        writer.write(report);
                        writer.write("\n");
                    } catch (IOException e) {
                        throw new GrobidException("Error when saving evaluation results into files. ", e);
                    }
                } else {
                    System.out.println(report);
                }
            }
        } else {
            System.out.println("Cannot find the specified model: " + modelName + ".");
        }

    }
}