package it.univaq.f4i.iw.examples;

import it.univaq.f4i.iw.framework.result.StreamResult;
import it.univaq.f4i.iw.framework.security.SecurityHelpers;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Giuseppe Della Penna
 */
public class GetImage extends HttpServlet {

    //questo metodo crea un'immagine PNG al volo in memoria
    //this method creates an in-memory PNG image on the fly
    private byte[] make_image(String text, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.blue);
        g.fillRect(0, 0, width, height);

        Font font = new Font("Times", Font.BOLD, 100);
        FontMetrics metrics = g.getFontMetrics(font);
        int x = (width - metrics.stringWidth(text)) / 2;
        int y = ((height - metrics.getHeight()) / 2) + metrics.getAscent();

        g.setFont(font);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.white);
        g.drawString(text, x, y);

        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageWriter imgWrtr = ImageIO.getImageWritersByFormatName("png").next();

        try (ImageOutputStream imgOutStrm = ImageIO.createImageOutputStream(baos)) {
            imgWrtr.setOutput(imgOutStrm);
            ImageWriteParam pngWrtPrm = imgWrtr.getDefaultWriteParam();
            pngWrtPrm.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            pngWrtPrm.setCompressionQuality(1);
            imgWrtr.write(null, new IIOImage(image, null, null), pngWrtPrm);
        }

        return baos.toByteArray();
    }

    private void action_error(HttpServletRequest request, HttpServletResponse response) {
        //ad esempio, possiamo caricare un'immagine di default se non riusciamo a trovare quella indicata
        //as an example, we can send a default image if we cannot find the requested one

        try {
            StreamResult result = new StreamResult(getServletContext());
            request.setAttribute("contentDisposition", "inline");
            request.setAttribute("contentType", "image/png");
            byte[] image = make_image("404", 200, 200);
            result.setResource(new ByteArrayInputStream(image), image.length, "err_image");
            result.activate(request, response);
        } catch (IOException ex) {
            //if the error image cannot be sent, try a standard HTTP error message
            //se non possiamo inviare l'immagine di errore, proviamo un messaggio di errore HTTP standard
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
            } catch (IOException ex1) {
                //if ALSO this error status cannot be notified, write to the server log
                //se ANCHE questo stato di errore non può essere notificato, scriviamo sul log del server
                Logger.getLogger(GetImage.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }
    }

    //questo metodo permette di scaricare un'immagine creata al volo
    //this method downloads an image generated on the fly
    private void action_generate_download(HttpServletRequest request, HttpServletResponse response) throws IOException {
        StreamResult result = new StreamResult(getServletContext());
        //estraiamo la resource ID dalla request (assumiamo che ci sia!)
        //extract the resource ID from the request object (we assume that there is one!)
        byte[] image = make_image(request.getAttribute("resource").toString(), 200, 200);

        //il file non sarà scaricato, ma mostrato nel browser
        //this file will not be saved, but displayed in the browser window
        request.setAttribute("contentDisposition", "inline");
        request.setAttribute("contentType", "image/png");
        result.setResource(new ByteArrayInputStream(image), image.length, "image");
        result.activate(request, response);

    }

    //questo metodo permette di scaricare un'immagine letta da disco
    //this method downloads an image read from disk
    private void action_download(HttpServletRequest request, HttpServletResponse response) throws IOException {
        StreamResult result = new StreamResult(getServletContext());
        //estraiamo la resource ID dalla request (assumiamo che ci sia!)
        //extract the resource ID from the request object (we assume that there is one!)
        int imgid = (Integer) request.getAttribute("resource");

        //usate questa versione solo se siete certi che il WAR sia stato espanso sul disco, o per caricare l'immagine da una cartella esterna al contesto
        //use this version only if you know that the WAR file will be expanded to disk, or to load the image from a folder outside the context
        //File in = new File(getServletContext().getRealPath("") + File.separatorChar + "images" + File.separatorChar + "IMG_" + imgid + ".jpg");
        //
        //questa versione funziona sempre quando la risorsa è all'interno del WAR
        //this version always works when the resource is inside the WAR
        URL resource = getServletContext().getResource("/images/" + "IMG_" + imgid + ".jpg");
        if (resource != null) {
            //il file non sarà scaricato, ma mostrato nel browser
            //this file will not be saved, but displayed in the browser window
            request.setAttribute("contentDisposition", "inline");
            request.setAttribute("contentType", "image/jpeg");
            result.setResource(resource);
            result.activate(request, response);
        } else {
            //oppure potremmo caricare un'immagine di default
            //or we may load a "default" image
            action_error(request, response);
        }

    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            int imgid = SecurityHelpers.checkNumeric(request.getParameter("imgid"));
            request.setAttribute("resource", imgid);
            action_generate_download(request, response); //image created on the fly
            //action_download(request, response); //image from disk
        } catch (NumberFormatException | IOException ex) {
            action_error(request, response);
        }

    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "An image download servlet";
    }// </editor-fold>
}
