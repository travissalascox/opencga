package org.opencb.opencga.catalog.models.update;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.opencb.biodata.models.clinical.interpretation.DiseasePanel;
import org.opencb.biodata.models.commons.Phenotype;
import org.opencb.commons.datastore.core.ObjectMap;
import org.opencb.opencga.catalog.exceptions.CatalogException;

import java.util.List;
import java.util.Map;

import static org.opencb.opencga.core.common.JacksonUtils.getUpdateObjectMapper;

public class PanelUpdateParams {

    private String id;
    private String name;
    private String description;
    @Deprecated
    private String author;
    private DiseasePanel.SourcePanel source;
    private List<DiseasePanel.PanelCategory> categories;
    private List<String> tags;
    private List<Phenotype> phenotypes;
    private List<DiseasePanel.VariantPanel> variants;
    private List<DiseasePanel.GenePanel> genes;
    private List<DiseasePanel.RegionPanel> regions;
    private List<DiseasePanel.STR> strs;
    private Map<String, Integer> stats;
    private Map<String, Object> attributes;

    public PanelUpdateParams() {
    }

    public ObjectMap getUpdateMap() throws CatalogException {
        try {
            return new ObjectMap(getUpdateObjectMapper().writeValueAsString(this));
        } catch (JsonProcessingException e) {
            throw new CatalogException(e);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PanelUpdateParams{");
        sb.append("id='").append(id).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append(", author='").append(author).append('\'');
        sb.append(", source=").append(source);
        sb.append(", categories=").append(categories);
        sb.append(", tags=").append(tags);
        sb.append(", phenotypes=").append(phenotypes);
        sb.append(", variants=").append(variants);
        sb.append(", genes=").append(genes);
        sb.append(", regions=").append(regions);
        sb.append(", strs=").append(strs);
        sb.append(", stats=").append(stats);
        sb.append(", attributes=").append(attributes);
        sb.append('}');
        return sb.toString();
    }

    public String getId() {
        return id;
    }

    public PanelUpdateParams setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public PanelUpdateParams setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public PanelUpdateParams setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getAuthor() {
        return author;
    }

    public PanelUpdateParams setAuthor(String author) {
        this.author = author;
        return this;
    }

    public DiseasePanel.SourcePanel getSource() {
        return source;
    }

    public PanelUpdateParams setSource(DiseasePanel.SourcePanel source) {
        this.source = source;
        return this;
    }

    public List<DiseasePanel.PanelCategory> getCategories() {
        return categories;
    }

    public PanelUpdateParams setCategories(List<DiseasePanel.PanelCategory> categories) {
        this.categories = categories;
        return this;
    }

    public List<String> getTags() {
        return tags;
    }

    public PanelUpdateParams setTags(List<String> tags) {
        this.tags = tags;
        return this;
    }

    public List<Phenotype> getPhenotypes() {
        return phenotypes;
    }

    public PanelUpdateParams setPhenotypes(List<Phenotype> phenotypes) {
        this.phenotypes = phenotypes;
        return this;
    }

    public List<DiseasePanel.VariantPanel> getVariants() {
        return variants;
    }

    public PanelUpdateParams setVariants(List<DiseasePanel.VariantPanel> variants) {
        this.variants = variants;
        return this;
    }

    public List<DiseasePanel.GenePanel> getGenes() {
        return genes;
    }

    public PanelUpdateParams setGenes(List<DiseasePanel.GenePanel> genes) {
        this.genes = genes;
        return this;
    }

    public List<DiseasePanel.RegionPanel> getRegions() {
        return regions;
    }

    public PanelUpdateParams setRegions(List<DiseasePanel.RegionPanel> regions) {
        this.regions = regions;
        return this;
    }

    public List<DiseasePanel.STR> getStrs() {
        return strs;
    }

    public PanelUpdateParams setStrs(List<DiseasePanel.STR> strs) {
        this.strs = strs;
        return this;
    }

    public Map<String, Integer> getStats() {
        return stats;
    }

    public PanelUpdateParams setStats(Map<String, Integer> stats) {
        this.stats = stats;
        return this;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public PanelUpdateParams setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
        return this;
    }
}
