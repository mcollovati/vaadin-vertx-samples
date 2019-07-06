package com.github.mcollovati.vertxvaadin.flowdemo.crud;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import com.github.mcollovati.vertxvaadin.flowdemo.SerializableUtils;
import com.github.mcollovati.vertxvaadin.flowdemo.backend.data.Availability;
import com.github.mcollovati.vertxvaadin.flowdemo.backend.data.Category;
import com.github.mcollovati.vertxvaadin.flowdemo.backend.data.Product;
import com.vaadin.flow.component.AbstractCompositeField;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.converter.StringToBigDecimalConverter;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.server.StreamResource;
import org.vaadin.pekka.CheckboxGroup;

/**
 * A form for editing a single product.
 */
public class ProductForm extends Div {

    private VerticalLayout content;

    private TextField productName;
    private TextField price;
    private TextField stockCount;
    private ComboBox<Availability> availability;
    private CheckboxGroup<Category> category;
    private ImageField image;
    private Button save;
    private Button discard;
    private Button cancel;
    private Button delete;

    private SampleCrudLogic viewLogic;
    private Binder<Product> binder;
    private Product currentProduct;
    private final Upload upload;

    private static class PriceConverter extends StringToBigDecimalConverter {

        public PriceConverter() {
            super(BigDecimal.ZERO, "Cannot convert value to a number.");
        }

        @Override
        protected NumberFormat getFormat(Locale locale) {
            // Always display currency with two decimals
            NumberFormat format = super.getFormat(locale);
            if (format instanceof DecimalFormat) {
                format.setMaximumFractionDigits(2);
                format.setMinimumFractionDigits(2);
            }
            return format;
        }
    }

    private static class StockCountConverter extends StringToIntegerConverter {

        public StockCountConverter() {
            super(0, "Could not convert value to " + Integer.class.getName()
                + ".");
        }

        @Override
        protected NumberFormat getFormat(Locale locale) {
            // Do not use a thousands separator, as HTML5 input type
            // number expects a fixed wire/DOM number format regardless
            // of how the browser presents it to the user (which could
            // depend on the browser locale).
            DecimalFormat format = new DecimalFormat();
            format.setMaximumFractionDigits(0);
            format.setDecimalSeparatorAlwaysShown(false);
            format.setParseIntegerOnly(true);
            format.setGroupingUsed(false);
            return format;
        }
    }

    public ProductForm(SampleCrudLogic sampleCrudLogic) {
        setClassName("product-form");

        content = new VerticalLayout();
        content.setSizeUndefined();
        add(content);

        viewLogic = sampleCrudLogic;

        productName = new TextField("Product name");
        productName.setWidth("100%");
        productName.setRequired(true);
        productName.setValueChangeMode(ValueChangeMode.EAGER);
        content.add(productName);

        SerializableUtils.FileReceiver fileBuffer = SerializableUtils.newFileBuffer();

        upload = new Upload(fileBuffer);
        upload.setAcceptedFileTypes("image/*");
        upload.addSucceededListener(event -> {
            Path path = saveFile(fileBuffer);
            image.setValue(path.toAbsolutePath().toString());
        });
        content.add(upload);

        image = new ImageField();
        image.setWidth("200px");
        content.add(image);

        price = new TextField("Price");
        price.setSuffixComponent(new Span("€"));
        price.getElement().getThemeList().add("align-right");
        price.setValueChangeMode(ValueChangeMode.EAGER);

        stockCount = new TextField("In stock");
        stockCount.getElement().getThemeList().add("align-right");
        stockCount.setValueChangeMode(ValueChangeMode.EAGER);

        HorizontalLayout horizontalLayout = new HorizontalLayout(price,
            stockCount);
        horizontalLayout.setWidth("100%");
        horizontalLayout.setFlexGrow(1, price, stockCount);
        content.add(horizontalLayout);

        availability = new ComboBox<>("Availability");
        availability.setWidth("100%");
        availability.setRequired(true);
        availability.setItems(Availability.values());
        availability.setAllowCustomValue(false);
        content.add(availability);

        category = new CheckboxGroup<>();
        category.setId("category");
        category.getContent().getStyle().set("flex-direction", "column")
            .set("margin", "0");
        Label categoryLabel = new Label("Categories");
        categoryLabel.setClassName("vaadin-label");
        categoryLabel.setFor(category);
        content.add(categoryLabel, category);

        binder = new BeanValidationBinder<>(Product.class);
        binder.forField(price).withConverter(new PriceConverter())
            .bind("price");
        binder.forField(stockCount).withConverter(new StockCountConverter())
            .bind("stockCount");
        binder.bindInstanceFields(this);

        // enable/disable save button while editing
        binder.addStatusChangeListener(event -> {
            boolean isValid = !event.hasValidationErrors();
            boolean hasChanges = binder.hasChanges();
            save.setEnabled(hasChanges && isValid);
            discard.setEnabled(hasChanges);
        });

        save = new Button("Save");
        save.setWidth("100%");
        save.getElement().getThemeList().add("primary");
        save.addClickListener(event -> {
            if (currentProduct != null
                && binder.writeBeanIfValid(currentProduct)) {
                viewLogic.saveProduct(currentProduct);
            }
        });

        discard = new Button("Discard changes");
        discard.setWidth("100%");
        discard.addClickListener(
            event -> viewLogic.editProduct(currentProduct));

        cancel = new Button("Cancel");
        cancel.setWidth("100%");
        cancel.addClickListener(event -> viewLogic.cancelProduct());
        getElement()
            .addEventListener("keydown", event -> viewLogic.cancelProduct())
            .setFilter("event.key == 'Escape'");

        delete = new Button("Delete");
        delete.setWidth("100%");
        delete.getElement().getThemeList()
            .addAll(Arrays.asList("error", "primary"));
        delete.addClickListener(event -> {
            if (currentProduct != null) {
                viewLogic.deleteProduct(currentProduct);
            }
        });

        content.add(save, discard, delete, cancel);
    }

    private Path saveFile(SerializableUtils.FileReceiver fileBuffer) {
        try {
            Path file = Files.createTempFile("product-", fileBuffer.getFileName());
            Files.copy(fileBuffer.getInputStream(), file, StandardCopyOption.REPLACE_EXISTING);
            return file;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setCategories(Collection<Category> categories) {
        category.setItems(categories);
    }

    public void editProduct(Product product) {
        if (product == null) {
            product = new Product();
        }
        upload.getElement().executeJavaScript("this.files=[]");
        delete.setVisible(!product.isNewProduct());
        currentProduct = product;
        binder.readBean(product);
    }
}

class ImageField extends AbstractCompositeField<Image, ImageField, String> implements HasSize {

    public ImageField() {
        super(null);
    }

    @Override
    protected void setPresentationValue(String newPresentationValue) {
        if (newPresentationValue != null) {
            File file = new File(newPresentationValue);
            getContent().setSrc(new StreamResource(file.getName(), () -> {
                try {
                    return new FileInputStream(file);
                } catch (Exception ex) { throw new RuntimeException(ex); }
            }));
        } else {
            getContent().setSrc("");
        }
    }
}