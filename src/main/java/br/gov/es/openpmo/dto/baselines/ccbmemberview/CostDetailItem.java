package br.gov.es.openpmo.dto.baselines.ccbmemberview;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static br.gov.es.openpmo.dto.baselines.ccbmemberview.TripleConstraintUtils.ONE_HUNDRED;
import static br.gov.es.openpmo.dto.baselines.ccbmemberview.TripleConstraintUtils.roundOneDecimal;

public class CostDetailItem {

  private final String icon;
  private final String description;
  private final BigDecimal currentValue;
  private final BigDecimal proposedValue;
  private BigDecimal variation;

  public CostDetailItem(
    final String icon,
    final String description,
    final BigDecimal currentValue,
    final BigDecimal proposedValue
  ) {
    this.icon = icon;
    this.description = description;
    this.currentValue = currentValue;
    this.proposedValue = proposedValue;
    this.calculateVariation();
  }

  private void calculateVariation() {
    if(this.currentValue == null || this.proposedValue == null) {
      return;
    }
    this.variation = this.currentValue
      .subtract(this.proposedValue)
      .divide(this.currentValue, 6, RoundingMode.HALF_UP)
      .multiply(ONE_HUNDRED);
  }

  public BigDecimal getVariation() {
    this.calculateVariation();
    return this.variation;
  }

  public String getIcon() {
    return this.icon;
  }

  public String getDescription() {
    return this.description;
  }

  public BigDecimal getCurrentValue() {
    return this.currentValue;
  }

  public BigDecimal getProposedValue() {
    return this.proposedValue;
  }

  public void roundData() {
    this.variation = roundOneDecimal(this.variation);
  }

}
