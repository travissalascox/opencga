/*
 * Copyright 2015-2020 OpenCB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opencb.opencga.analysis.variant.mutationalSignature;

import htsjdk.samtools.reference.BlockCompressedIndexedFastaSequenceFile;
import htsjdk.samtools.reference.FastaSequenceIndex;
import htsjdk.samtools.reference.ReferenceSequence;
import htsjdk.samtools.util.GZIIndex;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.opencb.biodata.models.variant.Variant;
import org.opencb.commons.datastore.core.ObjectMap;
import org.opencb.commons.datastore.core.Query;
import org.opencb.commons.datastore.core.QueryOptions;
import org.opencb.commons.utils.DockerUtils;
import org.opencb.opencga.analysis.ResourceUtils;
import org.opencb.opencga.analysis.variant.manager.VariantCatalogQueryUtils;
import org.opencb.opencga.analysis.variant.manager.VariantStorageManager;
import org.opencb.opencga.analysis.variant.manager.VariantStorageToolExecutor;
import org.opencb.opencga.catalog.exceptions.CatalogException;
import org.opencb.opencga.core.common.JacksonUtils;
import org.opencb.opencga.core.exceptions.ToolException;
import org.opencb.opencga.core.exceptions.ToolExecutorException;
import org.opencb.opencga.core.response.OpenCGAResult;
import org.opencb.opencga.core.tools.annotations.ToolExecutor;
import org.opencb.opencga.core.tools.variant.MutationalSignatureAnalysisExecutor;
import org.opencb.opencga.storage.core.exceptions.StorageEngineException;
import org.opencb.opencga.storage.core.variant.adaptors.VariantQueryParam;
import org.opencb.opencga.storage.core.variant.adaptors.iterators.VariantDBIterator;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.opencb.opencga.analysis.variant.mutationalSignature.MutationalSignatureAnalysis.SIGNATURES_FILENAME;

@ToolExecutor(id="opencga-local", tool = MutationalSignatureAnalysis.ID,
        framework = ToolExecutor.Framework.LOCAL, source = ToolExecutor.Source.STORAGE)
public class MutationalSignatureLocalAnalysisExecutor extends MutationalSignatureAnalysisExecutor implements VariantStorageToolExecutor {

    public final static String R_DOCKER_IMAGE = "opencb/opencga-r:2.0.0-dev";

    public final static String CONTEXT_FILENAME = "context.txt";

    @Override
    public void run() throws ToolException, IOException {
        // Context index filename
        File indexFile = getOutDir().resolve(getContextIndexFilename(getSampleName())).toFile();
        PrintWriter pw = new PrintWriter(indexFile);

        try {
            long start, faiTime = 0;
            StopWatch prepIteratorWatch = new StopWatch();
            StopWatch rWatch = new StopWatch();
            StopWatch loopWatch = new StopWatch();
            StopWatch totalWatch = new StopWatch();
            totalWatch.start();

            // Compute signature profile: contextual frequencies of each type of base substitution

            Query query = new Query()
                    .append(VariantQueryParam.STUDY.key(), getStudy())
                    .append(VariantQueryParam.SAMPLE.key(), getSampleName())
                    .append(VariantQueryParam.TYPE.key(), "SNV");

            QueryOptions queryOptions = new QueryOptions(QueryOptions.INCLUDE, "id");

            // Get variant iterator
            prepIteratorWatch.start();
            VariantDBIterator iterator = getVariantIterator(query, queryOptions);
            prepIteratorWatch.stop();

            // Read mutation context from reference genome (.gz, .gz.fai and .gz.gzi files)
            String base = getRefGenomePath().toAbsolutePath().toString();
            BlockCompressedIndexedFastaSequenceFile indexed = new BlockCompressedIndexedFastaSequenceFile(getRefGenomePath(),
                    new FastaSequenceIndex(new File(base + ".fai")), GZIIndex.loadIndex(Paths.get(base + ".gzi")));

            Map<String, Map<String, Double>> countMap = initFreqMap();

            long ko = 0;
            long count = 0;
            long contextCount = 0;
            loopWatch.start();

            while (iterator.hasNext()) {
                count++;
                Variant variant = iterator.next();

                // FAI access time
                String key = variant.getReference() + ">" + variant.getAlternate();
                if (countMap.containsKey(key)) {
                    contextCount++;
                    try {
                        start = System.currentTimeMillis();
                        ReferenceSequence refSeq = indexed.getSubsequenceAt(variant.getChromosome(), variant.getStart() - 1,
                                variant.getEnd() + 1);
                        String sequence = new String(refSeq.getBases());
                        faiTime += (System.currentTimeMillis() - start);

                        // Write context index
                        pw.println(variant.toString() + "\t" + sequence);

                        // Update context counts
                        if (countMap.get(key).containsKey(sequence)) {
                            countMap.get(key).put(sequence, countMap.get(key).get(sequence) + 1);
                        }
                    } catch (Exception e) {
                        //System.out.println("Error getting context sequence for variant " + variant.toStringSimple() + ": " + e.getMessage());
                        ko++;
                    }
                }
            }
            loopWatch.stop();

            // Write context counts
            writeCountMap(countMap, getOutDir().resolve(CONTEXT_FILENAME).toFile());

            // Close context index file
            pw.close();

            // Execute R script in docker
            rWatch.start();
            executeRScript(true);
            rWatch.stop();

            totalWatch.stop();

            System.out.println("number of variants = " + count);
            System.out.println("number of variantes in context = " + contextCount);
            System.out.println("number of errors when accessing context = " + ko);
            System.out.println("get iterator time = " + prepIteratorWatch.getTime());
            System.out.println("FAI time = " + faiTime);
            System.out.println("loop time (iterator time + FAI time + ...) = " + loopWatch.getTime());
            System.out.println("R script time = " + rWatch.getTime());
            System.out.println("Total time = " + totalWatch.getTime());
        } catch (Exception e) {
            throw new ToolExecutorException(e);
        }


        // Check output files
        if (!new File(getOutDir() + "/signature_summary.png").exists()
                || !new File(getOutDir() + "/signature_coefficients.json").exists()) {
            String msg = "Something wrong executing mutational signature.";
            throw new ToolException(msg);
        }
    }

    public MutationalSignatureResult query(Query query, QueryOptions queryOptions)
            throws CatalogException, ToolException, StorageEngineException, IOException {

        File signatureFile = ResourceUtils.downloadAnalysis(MutationalSignatureAnalysis.ID, SIGNATURES_FILENAME, getOutDir());
        if (signatureFile == null) {
            throw new ToolException("Error downloading mutational signatures file from " + ResourceUtils.URL);
        }
        setMutationalSignaturePath(signatureFile.toPath());

        StopWatch prepIteratorWatch = new StopWatch();
        StopWatch rWatch = new StopWatch();
        StopWatch loopWatch = new StopWatch();
        StopWatch totalWatch = new StopWatch();
        totalWatch.start();

        // Get context index filename
        String name = getContextIndexFilename(getSampleName());
        Query fileQuery = new Query("name", name);
        QueryOptions fileQueryOptions = new QueryOptions("include", "uri");
        OpenCGAResult<org.opencb.opencga.core.models.file.File> fileResult = getVariantStorageManager().getCatalogManager()
                .getFileManager().search(getStudy(), fileQuery, fileQueryOptions, getToken());

        if (CollectionUtils.isEmpty(fileResult.getResults())) {
            throw new ToolException("Missing mutational signature context index file for sample " + getSampleName() + " in catalog");
        }

        File indexFile = null;
        long maxSize = 0;
        for (org.opencb.opencga.core.models.file.File file : fileResult.getResults()) {
            File auxFile = new File(file.getUri().getPath());
            if (auxFile.exists() && auxFile.length() > maxSize) {
                maxSize = auxFile.length();
                indexFile = auxFile;
            }
        }
        if (indexFile == null) {
            throw new ToolException("Missing muational signature context index file for sample " + getSampleName());
        }

        // Read context index
        long start = System.currentTimeMillis();
        Map<String, String> indexMap = new HashMap<>();
        BufferedReader br = new BufferedReader( new FileReader(indexFile));
        String line;
        while ( (line = br.readLine()) != null ){
            String[] parts = line.split("\t");
            indexMap.put(parts[0], parts[1]);
        }
        long loadTime = System.currentTimeMillis() - start;

        // Get variant iterator
        prepIteratorWatch.start();
        query.append(VariantQueryParam.TYPE.key(), "SNV");
        queryOptions.append(QueryOptions.INCLUDE, "id");
        VariantDBIterator iterator = getVariantIterator(query, queryOptions);
        prepIteratorWatch.stop();

        Map<String, Map<String, Double>> countMap = initFreqMap();

        long ko = 0;
        long count = 0;
        long contextCount = 0;
        loopWatch.start();

        while (iterator.hasNext()) {
            count++;
            Variant variant = iterator.next();

            // FAI access time
            String key = variant.getReference() + ">" + variant.getAlternate();
            if (countMap.containsKey(key)) {
                contextCount++;
                try {
                    // Read context index
                    String sequence = indexMap.get(variant.toString());

                    // Update context counts
                    if (countMap.get(key).containsKey(sequence)) {
                        countMap.get(key).put(sequence, countMap.get(key).get(sequence) + 1);
                    }
                } catch (Exception e) {
                    //System.out.println("Error getting context sequence for variant " + variant.toStringSimple() + ": " + e.getMessage());
                    ko++;
                }
            }
        }

        // Write context counts
        writeCountMap(countMap, getOutDir().resolve(CONTEXT_FILENAME).toFile());

        // Run R script
        rWatch.start();
        executeRScript(getExecutorParams().getBoolean("image"));
        rWatch.stop();

        totalWatch.stop();

        System.out.println("number of variants = " + count);
        System.out.println("number of variantes in context = " + contextCount);
        System.out.println("number of errors when accessing context = " + ko);
        System.out.println("index size = " + indexMap.size());
        System.out.println("load index time = " + loadTime);
        System.out.println("get iterator time = " + prepIteratorWatch.getTime());
        System.out.println("loop time (iterator time + Map time + ...) = " + loopWatch.getTime());
        System.out.println("R script time = " + rWatch.getTime());
        System.out.println("Total time = " + totalWatch.getTime());

        return parse(getOutDir());
    }

    private VariantDBIterator getVariantIterator(Query query, QueryOptions queryOptions) throws ToolExecutorException, CatalogException,
            StorageEngineException {
        VariantStorageManager storageManager = getVariantStorageManager();

        // Compute signature profile: contextual frequencies of each type of base substitution
        return storageManager.iterator(query, queryOptions, getToken());
    }

    private String executeRScript(boolean computeImage) throws IOException {
        String rScriptPath = getExecutorParams().getString("opencgaHome") + "/analysis/R/" + getToolId();
        List<AbstractMap.SimpleEntry<String, String>> inputBindings = new ArrayList<>();
        inputBindings.add(new AbstractMap.SimpleEntry<>(rScriptPath, "/data/input"));
        AbstractMap.SimpleEntry<String, String> outputBinding = new AbstractMap.SimpleEntry<>(getOutDir().toAbsolutePath().toString(),
                "/data/output");
        String scriptParams = "R CMD Rscript --vanilla /data/input/mutational-signature.r /data/output/" + CONTEXT_FILENAME + " "
                + "/data/output/" + SIGNATURES_FILENAME + " /data/output " + (computeImage ? "1" : "0");

        String cmdline = DockerUtils.run(R_DOCKER_IMAGE, inputBindings, outputBinding, scriptParams, null);
        System.out.println("Docker command line: " + cmdline);

        return cmdline;
    }

    private MutationalSignatureResult parse(Path dir) throws IOException {
        MutationalSignatureResult result = new MutationalSignatureResult();

        // Context counts
        Map<String, Integer> counts = new HashMap<>();
        File contextFile = dir.resolve("context.txt").toFile();
        if (contextFile.exists()) {
            List<String> lines = FileUtils.readLines(contextFile, Charset.defaultCharset());
            for (int i = 1; i < lines.size(); i++) {
                String[] fields = lines.get(i).split("\t");
                counts.put(fields[2], Math.round(Float.parseFloat((fields[3]))));
            }
            result.setCounts(counts);
        }

        // Signatures coefficients
        File coeffsFile = dir.resolve("signature_coefficients.json").toFile();
        if (coeffsFile.exists()) {
            Map content = JacksonUtils.getDefaultObjectMapper().readValue(coeffsFile, Map.class);
            Map coefficients = (Map) content.get("coefficients");
            Map<String, Double> coeffs = new HashMap<>();
            for (Object key : coefficients.keySet()) {
                Number coeff = (Number) coefficients.get(key);
                coeffs.put((String) key, coeff.doubleValue());
            }
            result.setCoeffs(coeffs);
            result.setRss((Double) content.get("rss"));
        }

        // Signature summary image
        File imgFile = dir.resolve("signature_summary.png").toFile();
        if (imgFile.exists()) {
            FileInputStream fileInputStreamReader = new FileInputStream(imgFile);
            byte[] bytes = new byte[(int) imgFile.length()];
            fileInputStreamReader.read(bytes);
            result.setSummaryImg(new String(Base64.getEncoder().encode(bytes), "UTF-8"));
        }

        return result;
    }
}
