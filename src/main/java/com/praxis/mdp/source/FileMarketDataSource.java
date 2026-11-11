package com.praxis.mdp.source;

import com.praxis.mdp.config.MdpProperties;
import com.praxis.mdp.model.EodPrice;
import com.praxis.mdp.model.Instrument;
import com.praxis.mdp.model.InstrumentType;
import com.praxis.mdp.model.MarketDataRecord;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

/**
 * Stand-in for Asset Control: reads {@code <inputDir>/<INSTRUMENT_TYPE>_<yyyy-MM-dd>.csv}.
 * A missing file fails the step (strict mode). Malformed lines surface as parse exceptions that the
 * job's skip policy handles.
 */
@Component
public class FileMarketDataSource implements MarketDataSource {

    static final String[] COLUMNS = {"instrumentId", "instrumentType", "symbol", "currency", "maturityDate", "closePrice"};

    private final MdpProperties props;

    public FileMarketDataSource(MdpProperties props) {
        this.props = props;
    }

    /** File location convention; public so tests and docs can refer to it. */
    public Path fileFor(InstrumentType type, LocalDate businessDate) {
        return props.inputDir().resolve(type.name() + "_" + businessDate + ".csv");
    }

    @Override
    public ItemStreamReader<MarketDataRecord> reader(InstrumentType type, LocalDate businessDate) {
        FlatFileItemReader<MarketDataRecord> reader = new FlatFileItemReaderBuilder<MarketDataRecord>()
                .name("fileMarketDataReader-" + type)
                .resource(new FileSystemResource(fileFor(type, businessDate)))
                .linesToSkip(1)
                .delimited().names(COLUMNS)
                .fieldSetMapper(fs -> map(fs, businessDate))
                .strict(true)
                .saveState(true)
                .build();
        return reader;
    }

    private static MarketDataRecord map(FieldSet fs, LocalDate businessDate) {
        String maturity = fs.readString("maturityDate");
        String price = fs.readString("closePrice");
        Instrument instrument = new Instrument(
                fs.readString("instrumentId"),
                InstrumentType.valueOf(fs.readString("instrumentType")),
                fs.readString("symbol"),
                fs.readString("currency"),
                maturity.isBlank() ? null : LocalDate.parse(maturity));
        // A blank price is allowed here (it becomes null) so the processor can report it as a validation skip.
        BigDecimal closePrice = price.isBlank() ? null : new BigDecimal(price);
        return new MarketDataRecord(instrument, new EodPrice(businessDate, closePrice));
    }
}
