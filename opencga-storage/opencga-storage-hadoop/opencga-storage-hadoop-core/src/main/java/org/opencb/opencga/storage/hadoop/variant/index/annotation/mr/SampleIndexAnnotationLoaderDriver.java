package org.opencb.opencga.storage.hadoop.variant.index.annotation.mr;

import htsjdk.variant.vcf.VCFConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.mapreduce.TableInputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.opencb.biodata.models.core.Region;
import org.opencb.commons.datastore.core.ObjectMap;
import org.opencb.opencga.storage.core.metadata.VariantStorageMetadataManager;
import org.opencb.opencga.storage.core.variant.adaptors.VariantQueryParam;
import org.opencb.opencga.storage.hadoop.variant.AbstractVariantsTableDriver;
import org.opencb.opencga.storage.hadoop.variant.adaptors.VariantHBaseQueryParser;
import org.opencb.opencga.storage.hadoop.variant.adaptors.phoenix.VariantPhoenixHelper;
import org.opencb.opencga.storage.hadoop.variant.converters.HBaseToVariantConverter;
import org.opencb.opencga.storage.hadoop.variant.index.sample.SampleIndexDBLoader;
import org.opencb.opencga.storage.hadoop.variant.mr.VariantAlignedInputFormat;
import org.opencb.opencga.storage.hadoop.variant.mr.VariantMapReduceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * Created on 25/02/19.
 *
 * @author Jacobo Coll &lt;jacobo167@gmail.com&gt;
 */
public class SampleIndexAnnotationLoaderDriver extends AbstractVariantsTableDriver {

    private static final Logger LOGGER = LoggerFactory.getLogger(SampleIndexAnnotationLoaderDriver.class);
    public static final String SAMPLES = "samples";

    private List<Integer> sampleIds;
    private boolean hasGenotype;

    @Override
    protected Class<SampleIndexAnnotationLoaderMapper> getMapperClass() {
        return SampleIndexAnnotationLoaderMapper.class;
    }

    @Override
    protected Map<String, String> getParams() {
        Map<String, String> params = new HashMap<>();
        params.put("--" + SAMPLES, "<samples>");
        return params;
    }

    @Override
    protected void parseAndValidateParameters() throws IOException {
        super.parseAndValidateParameters();

        VariantStorageMetadataManager metadataManager = getMetadataManager();
        String samples = getParam(SAMPLES);
        if (StringUtils.isNotEmpty(samples)) {
            sampleIds = new LinkedList<>();
            for (String sample : samples.split(",")) {
                Integer sampleId = metadataManager.getSampleId(getStudyId(), sample);
                if (sampleId == null) {
                    throw new IllegalArgumentException("Sample '" + sample + "' not found.");
                }
                sampleIds.add(sampleId);
            }
        } else {
            sampleIds = metadataManager.getIndexedSamples(getStudyId());
        }

        ObjectMap attributes = metadataManager.getStudyMetadata(getStudyId()).getAttributes();
        hasGenotype = HBaseToVariantConverter.getFixedFormat(attributes).contains(VCFConstants.GENOTYPE_KEY);
    }

    @Override
    protected Job setupJob(Job job, String archiveTable, String variantTable) throws IOException {

        Scan scan = new Scan();

        String region = getConf().get(VariantQueryParam.REGION.key(), "");
        if (StringUtils.isNotEmpty(region)) {
            VariantHBaseQueryParser.addRegionFilter(scan, new Region(region));
        }

        for (Integer sampleId : sampleIds) {
            scan.addColumn(getHelper().getColumnFamily(), VariantPhoenixHelper.buildSampleColumnKey(getStudyId(), sampleId));
        }
        scan.addColumn(getHelper().getColumnFamily(), VariantPhoenixHelper.VariantColumn.FULL_ANNOTATION.bytes());

        SampleIndexAnnotationLoaderMapper.setHasGenotype(job, hasGenotype);

        VariantMapReduceUtil.configureMapReduceScan(scan, getConf());

        VariantMapReduceUtil.initTableMapperJob(job, variantTable,
                scan, getMapperClass(), VariantAlignedInputFormat.class);
        VariantAlignedInputFormat.setDelegatedInputFormat(job, TableInputFormat.class);
        VariantAlignedInputFormat.setBatchSize(job, SampleIndexDBLoader.BATCH_SIZE);

        VariantMapReduceUtil.setOutputHBaseTable(job, getTableNameGenerator().getSampleIndexTableName(getStudyId()));

        VariantMapReduceUtil.setNoneReduce(job);

        return job;
    }

    @Override
    protected String getJobOperationName() {
        return "sample_index_annotation_loader";
    }

    public static void main(String[] args) throws Exception {
        try {
            System.exit(new SampleIndexAnnotationLoaderDriver().privateMain(args, null));
        } catch (Exception e) {
            LOGGER.error("Error executing " + SampleIndexAnnotationLoaderDriver.class, e);
            System.exit(1);
        }
    }

    public static String[] buildArgs(String archiveTable, String variantsTable, int studyId, Collection<?> sampleIds, ObjectMap other) {
        ObjectMap params = new ObjectMap(other);
        params.put(SAMPLES, sampleIds);
        return AbstractVariantsTableDriver.buildArgs(archiveTable, variantsTable, studyId, null, params);
    }

}