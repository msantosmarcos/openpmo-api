package br.gov.es.openpmo.dto.workpack;

import br.gov.es.openpmo.model.properties.Currency;
import br.gov.es.openpmo.model.properties.Property;

import java.math.BigDecimal;

public class CurrencyDto extends PropertyDto {

    private BigDecimal value;

    public BigDecimal getValue() {
        return this.value;
    }

    public void setValue(final BigDecimal value) {
        this.value = value;
    }

    public static PropertyDto of(final Property property) {
        final CurrencyDto currencyDto = new CurrencyDto();
        currencyDto.setId(property.getId());
        currencyDto.setIdPropertyModel(property.getPropertyModelId());
        currencyDto.setValue(((Currency) property).getValue());
        return currencyDto;
    }
}
