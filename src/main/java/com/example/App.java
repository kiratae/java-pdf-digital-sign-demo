package com.example;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.security.auth.x500.X500Principal;

import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfArray;
import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfNumber;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.StampingProperties;
import com.itextpdf.kernel.pdf.annot.PdfAnnotation;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.signatures.BouncyCastleDigest;
import com.itextpdf.signatures.DigestAlgorithms;
import com.itextpdf.signatures.IExternalDigest;
import com.itextpdf.signatures.IExternalSignature;
import com.itextpdf.signatures.IExternalSignatureContainer;
import com.itextpdf.signatures.PdfSignatureAppearance;
import com.itextpdf.signatures.PdfSigner;
import com.itextpdf.signatures.PrivateKeySignature;
import com.itextpdf.text.pdf.AcroFields;
import com.itextpdf.text.pdf.security.PdfPKCS7;

import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * Hello world!
 *
 */
public class App {
        public static final String KEYSTORE = "keys\\TanapornKleaklom.pfx";
        public static final char[] PASSWORD = "password".toCharArray();
        public static final String SRC = "C:\\my\\temp\\hello.pdf";
        public static final String DEST = "C:\\my\\temp\\hello_signed%s.pdf";
        public static final String IMG = "./src/main/resources/img/logo.png";
        public static final String SIGNAME = "signature";

        public static void main(String[] args) throws GeneralSecurityException, IOException {
                System.out.println("Hello World!");
                BouncyCastleProvider provider = new BouncyCastleProvider();
                Security.addProvider(provider);
                KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
                ks.load(new FileInputStream(KEYSTORE), PASSWORD);
                String alias = (String) ks.aliases().nextElement();
                PrivateKey pk = (PrivateKey) ks.getKey(alias, PASSWORD);
                Certificate[] chain = ks.getCertificateChain(alias);
                ImageData image = ImageDataFactory.create(IMG);
                String location = "TH";

                createPdf(SRC);
                sign(SRC, SIGNAME, String.format(DEST, 1), chain, pk,
                                DigestAlgorithms.SHA256, provider.getName(), PdfSigner.CryptoStandard.CMS,
                                "Test 1", location, PdfSignatureAppearance.RenderingMode.DESCRIPTION, null);

                sign(SRC, SIGNAME, String.format(DEST, 2), chain, pk,
                                DigestAlgorithms.SHA256, provider.getName(), PdfSigner.CryptoStandard.CMS,
                                "Test 2", location, PdfSignatureAppearance.RenderingMode.NAME_AND_DESCRIPTION, null);

                sign(SRC, SIGNAME, String.format(DEST, 3), chain, pk,
                                DigestAlgorithms.SHA256, provider.getName(), PdfSigner.CryptoStandard.CMS,
                                "Test 3", location, PdfSignatureAppearance.RenderingMode.GRAPHIC_AND_DESCRIPTION,
                                image);

                sign(SRC, SIGNAME, String.format(DEST, 4), chain, pk,
                                DigestAlgorithms.SHA256, provider.getName(), PdfSigner.CryptoStandard.CMS,
                                "Test 4", location, PdfSignatureAppearance.RenderingMode.GRAPHIC, image);

                verify(String.format(DEST, 1));
                verify(String.format(DEST, 2));
                verify(String.format(DEST, 3));
                verify(String.format(DEST, 4));
        }

        public static void createPdf(String filename) throws IOException {
                PdfDocument pdfDoc = new PdfDocument(new PdfWriter(filename));
                Document doc = new Document(pdfDoc);

                doc.add(new Paragraph("Hello World!"));

                // Create a signature form field
                PdfFormField field = PdfFormField.createSignature(pdfDoc, new Rectangle(72, 632, 200, 100));
                field.setFieldName(SIGNAME);
                field.setPage(1);

                // Set the widget properties
                field.getWidgets().get(0).setHighlightMode(PdfAnnotation.HIGHLIGHT_INVERT)
                                .setFlags(PdfAnnotation.PRINT);

                PdfDictionary mkDictionary = field.getWidgets().get(0).getAppearanceCharacteristics();
                if (null == mkDictionary) {
                        mkDictionary = new PdfDictionary();
                }

                PdfArray black = new PdfArray();
                black.add(new PdfNumber(ColorConstants.BLACK.getColorValue()[0]));
                black.add(new PdfNumber(ColorConstants.BLACK.getColorValue()[1]));
                black.add(new PdfNumber(ColorConstants.BLACK.getColorValue()[2]));
                mkDictionary.put(PdfName.BC, black);

                PdfArray white = new PdfArray();
                white.add(new PdfNumber(ColorConstants.WHITE.getColorValue()[0]));
                white.add(new PdfNumber(ColorConstants.WHITE.getColorValue()[1]));
                white.add(new PdfNumber(ColorConstants.WHITE.getColorValue()[2]));
                mkDictionary.put(PdfName.BG, white);

                field.getWidgets().get(0).setAppearanceCharacteristics(mkDictionary);

                PdfAcroForm.getAcroForm(pdfDoc, true).addField(field);

                Rectangle rect = new Rectangle(0, 0, 200, 100);
                PdfFormXObject xObject = new PdfFormXObject(rect);
                PdfCanvas canvas = new PdfCanvas(xObject, pdfDoc);
                canvas
                                .setStrokeColor(ColorConstants.BLUE)
                                .setFillColor(ColorConstants.LIGHT_GRAY)
                                .rectangle(0 + 0.5, 0 + 0.5, 200 - 0.5, 100 - 0.5)
                                .fillStroke()
                                .setFillColor(ColorConstants.BLUE);
                try (Canvas c = new Canvas(canvas, rect)) {
                        c.showTextAligned("SIGN HERE", 100, 50,
                                        TextAlignment.CENTER, (float) Math.toRadians(25));
                }
                // Note that Acrobat doesn't show normal appearance in the highlight mode.
                field.getWidgets().get(0).setNormalAppearance(xObject.getPdfObject());

                doc.close();
        }

        public static boolean verify(String src) throws IOException,
                        GeneralSecurityException {
                boolean valid = false;
                com.itextpdf.text.pdf.PdfReader reader = new com.itextpdf.text.pdf.PdfReader(src);
                AcroFields acroFields = reader.getAcroFields();
                List<String> signatureNames = acroFields.getSignatureNames();
                if (!signatureNames.isEmpty()) {
                        for (String name : signatureNames) {
                                if (acroFields.signatureCoversWholeDocument(name)) {
                                        PdfPKCS7 pkcs7 = acroFields.verifySignature(name);
                                        valid = pkcs7.verify();
                                        String reason = pkcs7.getReason();
                                        Calendar signedAt = pkcs7.getSignDate();
                                        X509Certificate signingCertificate = pkcs7.getSigningCertificate();
                                        X500Principal pincipal = signingCertificate.getSubjectX500Principal();
                                        System.out.println(String.format(
                                                        "valid = %1$s, date = %2$s, reason = '%3$s', subject = '%4$s'",
                                                        valid, signedAt.getTime(), reason, pincipal.toString()));
                                        break;
                                }
                        }
                }
                return valid;
        }

        public static void sign(String src, String name, String dest, Certificate[] chain, PrivateKey pk,
                        String digestAlgorithm, String provider, PdfSigner.CryptoStandard subfilter,
                        String reason, String location, PdfSignatureAppearance.RenderingMode renderingMode,
                        ImageData image)
                        throws GeneralSecurityException, IOException {
                PdfReader reader = new PdfReader(src);

                // Pass the temporary file's path to the PdfSigner constructor
                PdfSigner signer = new PdfSigner(reader, new FileOutputStream(dest), new StampingProperties());

                // Create the signature appearance
                PdfSignatureAppearance appearance = signer.getSignatureAppearance();

                appearance.setReason(reason);
                appearance.setLocation(location);

                // This name corresponds to the name of the field that already exists in the
                // document.
                signer.setFieldName(name);

                // Set the custom text and a custom font
                appearance.setLayer2Text("Signed on " + new Date().toString());

                // Set the rendering mode for this signature.
                appearance.setRenderingMode(renderingMode);

                // Set the Image object to render when the rendering mode is set to
                // RenderingMode.GRAPHIC
                // or RenderingMode.GRAPHIC_AND_DESCRIPTION.
                appearance.setSignatureGraphic(image);

                IExternalSignature pks = new PrivateKeySignature(pk, digestAlgorithm, provider);
                IExternalDigest digest = new BouncyCastleDigest();

                // Sign the document using the detached mode, CMS or CAdES equivalent.
                signer.signDetached(digest, pks, chain, null, null, null, 0, subfilter);
        }
}
