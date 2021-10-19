package in.mcxiv.jpsd.structure.addend;

import in.mcxiv.jpsd.data.addend.AdditionalInfoKey;
import in.mcxiv.jpsd.data.addend.AdditionalLayerInfo;
import in.mcxiv.jpsd.data.addend.UnknownAdditionalLayerInfo;
import in.mcxiv.jpsd.data.addend.types.EffectsLayer;
import in.mcxiv.jpsd.data.addend.types.LayerAndMaskInfo;
import in.mcxiv.jpsd.data.addend.types.LayerID;
import in.mcxiv.jpsd.data.addend.types.UnicodeLayerName;
import in.mcxiv.jpsd.data.layer.LayerInfo;
import in.mcxiv.jpsd.data.sections.FileHeaderData;
import in.mcxiv.jpsd.exceptions.UnknownByteBlockException;
import in.mcxiv.jpsd.io.DataReader;
import in.mcxiv.jpsd.io.DataWriter;
import in.mcxiv.jpsd.io.PSDFileReader;
import in.mcxiv.jpsd.structure.SectionIO;
import in.mcxiv.jpsd.structure.addend.types.EffectsLayerIO;
import in.mcxiv.jpsd.structure.layer.LayerInfoIO;

import java.io.IOException;
import java.util.Arrays;

public class AdditionalLayerInfoIO extends SectionIO<AdditionalLayerInfo> {

    public final FileHeaderData.FileVersion version;

    public final LayerInfoIO LAYER_INFO_IO;

    public static final EffectsLayerIO EFFECTS_LAYER_IO = new EffectsLayerIO();

    public AdditionalLayerInfoIO(FileHeaderData.FileVersion version) {
        super(true);
        this.version = version;
        LAYER_INFO_IO = null;
    }

    public AdditionalLayerInfoIO(FileHeaderData.FileVersion version, LayerInfoIO LAYER_INFO_IO) {
        super(true);
        this.version = version;
        this.LAYER_INFO_IO = LAYER_INFO_IO;
    }

    @Override
    public AdditionalLayerInfo read(DataReader reader) throws IOException {

        byte[] signature = reader.verifySignature(PSDFileReader.ADDITIONAL_LAYER_INFO_SIGNATURE_SMALL, PSDFileReader.ADDITIONAL_LAYER_INFO_SIGNATURE_LONG,
                PSDFileReader.CORRUPTED_ADDITIONAL_LAYER_INFO_SIGNATURE_SMALL, PSDFileReader.CORRUPTED_ADDITIONAL_LAYER_INFO_SIGNATURE_LONG);

        if (Arrays.equals(signature, PSDFileReader.CORRUPTED_ADDITIONAL_LAYER_INFO_SIGNATURE_LONG))
            reader.stream.skipBytes(2);

        AdditionalInfoKey key = AdditionalInfoKey.of(reader.readBytes(4, true));

        boolean isLargeResource = Arrays.equals(PSDFileReader.ADDITIONAL_LAYER_INFO_SIGNATURE_LONG, signature);

        if (version.isLarge()) {
            if (key.isLarge())
                isLargeResource = true;
        }

        long size;
        if (isLargeResource) size = reader.stream.readLong();
        else size = reader.stream.readInt();

        switch (key) {

            case EFFECTS_KEY:
                return EFFECTS_LAYER_IO.read(reader);

            case LAYER_AND_MASK_INFO_16:
                LayerInfo layerInfo = LAYER_INFO_IO.read(reader, size);
                reader.skipToPadBy(size, 4);
                return new LayerAndMaskInfo(key, size, layerInfo);

            case LAYER_ID_KEY:
                int id = reader.stream.readInt();
                return new LayerID(id, size);

            case UNICODE_LAYER_NAME_KEY:
                String unicodeName = reader.readUnicodeString();

                int expectedLength = (int) (size - 4) / 2;
                // Is there any danger of \0 coming next?
                if (unicodeName.length() != expectedLength) {
                    if (expectedLength - unicodeName.length() == 1) {
                        PSDFileReader.out.println("Reading num bytes!");
                        reader.stream.skipBytes(2);
                    } else {
                        new Exception("The heck?").printStackTrace(PSDFileReader.out);
                        reader.stream.skipBytes(2 * (expectedLength - unicodeName.length()));
                    }
                }
                return new UnicodeLayerName(unicodeName, size);

            default:
                switch (unknownBytesStrategy.action) {
                    case ReadAll:
                        return new UnknownAdditionalLayerInfo(key, reader.readBytes((int) size, true), size);
                    case ExcludeData:
                        reader.skipAndPadBy4(size);
                        return new UnknownAdditionalLayerInfo(key, null, size);
                    case Skip:
                        reader.skipAndPadBy4(size);
                        return null;
                    case Quit:
                        throw new UnknownByteBlockException("Layer And Mask Section> Additional Layer Info> " + key);
                }
        }

        System.err.println("HOW did i reach here?");
        return null;
    }

    @Override
    public void write(DataWriter writer, AdditionalLayerInfo additionalLayerInfo) {

    }
}
