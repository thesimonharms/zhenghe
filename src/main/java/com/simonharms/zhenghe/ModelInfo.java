package com.simonharms.zhenghe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Metadata about a model available from a provider.
 *
 * <p>Different providers return different subsets of this information.
 * Fields that are not returned by a provider will be {@code null}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModelInfo {

    @JsonProperty("id")
    private String id;

    @JsonProperty("owned_by")
    private String ownedBy;

    @JsonProperty("context_length")
    private Integer contextLength;

    @JsonProperty("display_name")
    private String displayName;

    public ModelInfo() {}

    /**
     * Constructs a ModelInfo with id and owner.
     *
     * @param id      the model identifier
     * @param ownedBy the organization that owns the model
     */
    public ModelInfo(String id, String ownedBy) {
        this.id = id;
        this.ownedBy = ownedBy;
    }

    /**
     * Constructs a ModelInfo with all fields.
     *
     * @param id            the model identifier
     * @param ownedBy       the organization that owns the model
     * @param contextLength the context window size in tokens (may be null)
     * @param displayName   a human-readable name (may be null)
     */
    public ModelInfo(
        String id,
        String ownedBy,
        Integer contextLength,
        String displayName
    ) {
        this.id = id;
        this.ownedBy = ownedBy;
        this.contextLength = contextLength;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOwnedBy() {
        return ownedBy;
    }

    public void setOwnedBy(String ownedBy) {
        this.ownedBy = ownedBy;
    }

    public Integer getContextLength() {
        return contextLength;
    }

    public void setContextLength(Integer contextLength) {
        this.contextLength = contextLength;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return (
            "ModelInfo{id='" +
            id +
            "', ownedBy='" +
            ownedBy +
            "', displayName='" +
            displayName +
            "'}"
        );
    }
}
