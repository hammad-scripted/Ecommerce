package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Category;
import com.ecommerce.project.model.Product;
import com.ecommerce.project.payload.ProductDTO;
import com.ecommerce.project.payload.ProductResponse;
import com.ecommerce.project.repositories.CategoryRepository;
import com.ecommerce.project.repositories.ProductRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class ProductServiceImpl implements ProductService{
    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private FileService fileService;

    @Value("${project.image}")
    private String path;



    @Override
    public ProductDTO addProduct(Long categoryId,  ProductDTO productDTO) {

       Category category=categoryRepository.findById(categoryId).orElseThrow(
               ()-> new ResourceNotFoundException(categoryId,"Category","categoryId")
       );
        //todo check if the product already present or not
        boolean ifProductNotPresent=true;
        List<Product>products=category.getProducts();
        for (Product value : products) {
            if (value.getProductName().equals(productDTO.getProductName())) {
                ifProductNotPresent = false;
                break;

            }
        }
        if(ifProductNotPresent) {
            Product product = modelMapper.map(productDTO, Product.class);
            product.setImage("Default.png");
            product.setCategory(category);
            double specialPrice = product.getPrice() - ((product.getDiscount() * 0.01) * (product.getPrice()));
            product.setSpecialPrice(specialPrice);
            Product savedProduct = productRepository.save(product);
            return modelMapper.map(savedProduct, ProductDTO.class);
        }
        else{
            throw new APIException("Product already exits!!!");
        }
    }



    @Override
    public ProductResponse getAllProducts(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        //! implementing sorting and pagination
        Sort sortByAndOrder=sortOrder.equalsIgnoreCase("asc")?Sort.by(sortBy).ascending():Sort.by(sortBy).descending();
        Pageable pageDetails= PageRequest.of(pageNumber,pageSize,sortByAndOrder);
        Page<Product> pageProducts=productRepository.findAll(pageDetails);
        //todo product size is zero
//      List<Product> products=productRepository.findAll();
        List<Product> products=pageProducts.getContent();
      List<ProductDTO> productDTOS=products.stream().map(
              product -> modelMapper.map(product,ProductDTO.class)
      ).toList();
      if(products.isEmpty()){
          throw new APIException("No product exists,please create one!");
      }
      ProductResponse productResponse=new ProductResponse();
      productResponse.setContent(productDTOS);
      productResponse.setPageNumber(pageProducts.getNumber());
      productResponse.setPageSize(pageProducts.getSize());
      productResponse.setTotalElements(pageProducts.getTotalElements());
      productResponse.setTotalPages(pageProducts.getTotalPages());
      productResponse.setLastPage(pageProducts.isLast());
      return productResponse;
    }



    @Override
    public ProductResponse searchByCategory(Long categoryId,Integer pageNumber,Integer pageSize,String  sortBy, String
            sortOrder) {
        Category category=categoryRepository.findById(categoryId).orElseThrow(
                ()->new ResourceNotFoundException(categoryId,"Category","categoryId")
        );
        Sort sortByAndOrder=sortOrder.equalsIgnoreCase("asc")?Sort.by(sortBy).ascending():Sort.by(sortBy).descending();
        Pageable pageDetails=PageRequest.of(pageNumber,pageSize,sortByAndOrder);
        Page<Product>pageProducts=productRepository.findByCategoryOrderByPriceAsc(category,pageDetails);
        List<Product> products=pageProducts.getContent();
        List<ProductDTO>productDTOS=products.stream().map(product->modelMapper.map(product,ProductDTO.class)).toList();
        if(products.isEmpty()){
            throw new APIException("No product exists,please create one!");
        }
        ProductResponse productResponse=new ProductResponse();
        productResponse.setContent(productDTOS);
        return productResponse;
    }

    @Override
    public ProductResponse searchProductByKeyword(String keyword,Integer pageNumber,Integer pageSize,String  sortBy, String sortOrder )

    {
        Sort sortByAndOrder=sortOrder.equalsIgnoreCase("asc")?Sort.by(sortBy).ascending():Sort.by(sortBy).descending();
        Pageable pageDetails=PageRequest.of(pageNumber,pageSize,sortByAndOrder);
        Page<Product> pageProducts=productRepository.findByProductNameLikeIgnoreCase("%"+keyword+"%",pageDetails);
//    List<Product>products=productRepository.findByProductNameLikeIgnoreCase("%"+keyword+"%");
        List<Product> products=pageProducts.getContent();

    List<ProductDTO> productDTOS=products.stream().map(
            product -> modelMapper.map(product,ProductDTO.class)
    ).toList();
        if(products.isEmpty()){
            throw new APIException("No product exists,please create one!");
        }
    ProductResponse productResponse=new ProductResponse();
    productResponse.setContent(productDTOS);
    productResponse.setPageNumber(pageProducts.getNumber());
    productResponse.setPageSize(pageProducts.getSize());
    productResponse.setTotalElements(pageProducts.getTotalElements());
    productResponse.setTotalPages(pageProducts.getTotalPages());
    productResponse.setLastPage(pageProducts.isLast());
    return productResponse;
    }

    @Override
    public ProductDTO updateProduct(Long productId, ProductDTO productDTO) {
        Product product=modelMapper.map(productDTO,Product.class);
      Product productFromDb=productRepository.findById(productId).orElseThrow(()->new ResourceNotFoundException(productId,"Product","product"));
      productFromDb.setProductName(product.getProductName());
      productFromDb.setDescription(product.getDescription());
      productFromDb.setQuantity(product.getQuantity());
      productFromDb.setDiscount(product.getDiscount());
      productFromDb.setPrice(product.getPrice());
      productFromDb.setSpecialPrice(product.getSpecialPrice());
      Product savedProduct=productRepository.save(productFromDb);
      return modelMapper.map(savedProduct,ProductDTO.class);

    }

    @Override
    public ProductDTO deleteProduct(Long productId) {
       Product product=productRepository.findById(productId).orElseThrow(()->new ResourceNotFoundException(productId,"Product","product"));
       productRepository.delete(product);
       return modelMapper.map(product,ProductDTO.class);
    }

    @Override
    public ProductDTO updateProductImage(Long productId, MultipartFile image) throws IOException {
     Product productFromDb=productRepository.findById(productId).orElseThrow(()->new ResourceNotFoundException(productId,"Product","product"));
        //? get the product from the db
        //? upload the image to the server

        String fileName=fileService.uploadImage(path,image);
        productFromDb.setImage(fileName);
        //? save the product
        Product updatedProduct=productRepository.save(productFromDb);
        return modelMapper.map(updatedProduct,ProductDTO.class);
        //? get the file name of the uploaded image

        //? updating the new file name to the product
        //? return dto after mapping product to dto
    }



}
